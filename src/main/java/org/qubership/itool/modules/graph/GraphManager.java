/*
 * Copyright 2024-2025 NetCracker Technology Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.qubership.itool.modules.graph;

import com.google.common.cache.*;
import org.qubership.itool.modules.artifactory.AppVersionDescriptor;
import org.qubership.itool.modules.artifactory.FailureStage;
import org.qubership.itool.modules.artifactory.GraphSnapshot;
import org.qubership.itool.modules.processor.GraphMerger;
import org.qubership.itool.modules.report.GraphReportImpl;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

import org.apache.commons.collections4.ListUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static org.qubership.itool.modules.processor.MergerApi.*;

import com.google.inject.Inject;

/**
 * Manager for graph operations.
 */
public class GraphManager {

    protected static final Logger LOG = LoggerFactory.getLogger(GraphManager.class);

    private final LoadingCache<String, GraphClassifier> graphClassifierCache;

    private final Vertx vertx;

    private boolean failFast;

    private final GraphFetcher graphFetcher;

    private final GraphFactory graphFactory;
    private final GraphReportFactory graphReportFactory;

    @Inject
    public GraphManager(Vertx vertx, GraphFetcher fetcher, boolean failFast, GraphFactory graphFactory, GraphReportFactory graphReportFactory) {
        this(vertx, fetcher, defaultClassifierCacheBuilder(), failFast, graphFactory, graphReportFactory);
    }

    @SuppressWarnings("rawtypes")
    protected static CacheBuilder defaultClassifierCacheBuilder() {
        return CacheBuilder.newBuilder()
            .concurrencyLevel(4)
            .maximumSize(16)
            .expireAfterAccess(Duration.ofDays(1));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public GraphManager(Vertx vertx, GraphFetcher fetcher,
            CacheBuilder classifierCacheBuilder, boolean failFast, GraphFactory graphFactory, GraphReportFactory graphReportFactory) {
        this.vertx = vertx;
        this.graphFetcher = fetcher;
        this.failFast = failFast;
        this.graphFactory = graphFactory;
        this.graphReportFactory = graphReportFactory;

        this.graphClassifierCache = classifierCacheBuilder
            .recordStats()
            .build(new ClassifierCacheLoader());
    }

    protected GraphClassifier resolveGraphClassifier(String graphClassifierId) {
        return graphFetcher.resolveGraphClassifier(graphClassifierId);
    }

    protected Graph buildGraphByClassifier(GraphClassifier classifier) {
        // TODO: thread safety
        this.graphClassifierCache.put(classifier.getId(), classifier);

        GraphSnapshot graphSnapshot = graphFetcher.fetchGraphDumpByClassifier(classifier);
        if (graphSnapshot != null && graphSnapshot.getGraphDump() != null) {
            return GraphDumpSupport.restoreFromJson(graphSnapshot.getGraphDump());
        }

        List<AppVersionDescriptor> allAppVersionIds = ListUtils.emptyIfNull(
            graphFetcher.fetchAllApplicationVersionIds(classifier)
        );

        Map<AppVersionDescriptor, GraphSnapshot> fetchedData = graphFetcher.fetchGraphDumpsByAppVersions(allAppVersionIds);
        Map<AppVersionDescriptor, GraphSnapshot> unprocessedAppIds = new HashMap<>();
        for (AppVersionDescriptor applicationVersionId: allAppVersionIds) {
            GraphSnapshot snapshot = fetchedData.get(applicationVersionId);
            if (snapshot == null) {
                snapshot = new GraphSnapshot();
            }
            if (snapshot.getGraphDump() == null && snapshot.getFailureStage() == null) {
                snapshot.setFailureStage(FailureStage.FETCHING);
            }
            if (snapshot.getFailureStage() != null) {
                unprocessedAppIds.put(applicationVersionId, snapshot);
                fetchedData.remove(applicationVersionId);
            }
        }

        Graph graph = new GraphImpl();
        if (classifier.isWithReport()) {
            graph.setReport(new GraphReportImpl());
        }

        if (!failFast || unprocessedAppIds.isEmpty()) {
            // Make merger throw exception for invalid graphs and catch them below
            try (GraphMerger merger = new GraphMerger(vertx, graphFactory)) {
                JsonObject targetInfo = new JsonObject();

                merger.prepareGraphForMerging(graph, targetInfo);
                Iterator<Map.Entry<AppVersionDescriptor, GraphSnapshot>> it = fetchedData.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<AppVersionDescriptor, GraphSnapshot> entry = it.next();
                    AppVersionDescriptor appId = entry.getKey();
                    GraphSnapshot snapshot = entry.getValue();

                    JsonObject sourceInfo = new JsonObject()
                        .put(P_IS_APPLICATION, true)
                        .put(P_APP_NAME, appId.getAppName())
                        .put(P_APP_VERSION, appId.getAppVersion());

                    try {
                        merger.mergeDump(snapshot.getGraphDump(), sourceInfo, graph, targetInfo);
                    } catch (Exception e) {
                        LOG.error("Failed to merge: " + appId, e);
                        it.remove();
                        unprocessedAppIds.put(appId, snapshot);
                        snapshot.setFailureStage(FailureStage.MERGING);
                        snapshot.setFailureDetails(e.toString());
                    }
                }

                merger.finalizeGraphAfterMerging(graph, targetInfo);
            }
        }

        if (failFast && ! unprocessedAppIds.isEmpty()) {
            List<String> artifactIds = unprocessedAppIds.keySet().stream()
                    .map(AppVersionDescriptor::asArtifactId).collect(Collectors.toList());
            throw new IllegalArgumentException("Invalid source graphs: " + artifactIds);
        }

        graphSnapshot = new GraphSnapshot();
        graphSnapshot.setGraphDump(GraphDumpSupport.dumpToJson(graph, false));

        this.graphFetcher.persistGraphByClassifier(classifier, graphSnapshot, fetchedData, unprocessedAppIds);

        return graph;
    }

    protected CacheStats getCacheStatistics() {
        return graphClassifierCache.stats();
    }

    public void invalidate(GraphClassifier graphClassifier) {
        this.graphClassifierCache.invalidate(graphClassifier.getId());
    }

    public void evictCache() {
        this.graphClassifierCache.invalidateAll();
    }

    // ========================================================================

    class ClassifierCacheLoader extends CacheLoader<String, GraphClassifier> {
        @Override
        public GraphClassifier load(String graphClassifierId) throws Exception {
            GraphClassifier graphClassifier = resolveGraphClassifier(graphClassifierId);
            if (graphClassifier == null) {
                throw new ExecutionException("GraphClassifier can't be loaded for: " + graphClassifierId, new NullPointerException());
            }
            return graphClassifier;
        }
    }

}
