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

import org.qubership.itool.modules.processor.MergerApi;
import org.qubership.itool.modules.report.GraphReport;

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

import jakarta.inject.Inject;
import jakarta.inject.Provider;

/**
 * Manager for graph operations.
 */
public class GraphManager {

    protected static final Logger LOG = LoggerFactory.getLogger(GraphManager.class);

    private final LoadingCache<String, GraphClassifier> graphClassifierCache;

    private boolean failFast;

    private final GraphFetcher graphFetcher;

    private final Provider<Graph> graphProvider;
    private final Provider<GraphReport> graphReportProvider;
    private final Provider<MergerApi> graphMergerProvider;

    @Inject
    public GraphManager(Vertx vertx, GraphFetcher fetcher, boolean failFast, Provider<Graph> graphProvider, Provider<GraphReport> graphReportProvider, Provider<MergerApi> graphMergerProvider) {
        this(vertx, fetcher, defaultClassifierCacheBuilder(), failFast, graphProvider, graphReportProvider, graphMergerProvider);
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
            CacheBuilder classifierCacheBuilder, boolean failFast, Provider<Graph> graphProvider, Provider<GraphReport> graphReportProvider, Provider<MergerApi> graphMergerProvider) {
        this.graphFetcher = fetcher;
        this.failFast = failFast;
        this.graphProvider = graphProvider;
        this.graphReportProvider = graphReportProvider;
        this.graphMergerProvider = graphMergerProvider;

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
            Graph graph = graphProvider.get();
            GraphDumpSupport.restoreFromJson(graph, graphSnapshot.getGraphDump());
            return graph;
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

        Graph graph = graphProvider.get();
        if (classifier.isWithReport()) {
            graph.setReport(graphReportProvider.get());
        }

        if (!failFast || unprocessedAppIds.isEmpty()) {
            // Make merger throw exception for invalid graphs and catch them below
            try (MergerApi merger = graphMergerProvider.get()) {
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
