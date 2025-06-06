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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.CacheStats;
import com.google.common.cache.LoadingCache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.ExecutionException;

public class GraphServiceImpl implements GraphService {

    protected static final Logger LOG = LoggerFactory.getLogger(GraphServiceImpl.class);

    private final GraphManager graphManager;

    private final LoadingCache<GraphClassifier, Graph> graphCache;

    public GraphServiceImpl(GraphManager graphManager) {
        this(graphManager, defaultGraphCacheBuilder());
    }

    @SuppressWarnings("rawtypes")
    protected static CacheBuilder defaultGraphCacheBuilder() {
        return CacheBuilder.newBuilder()
            .concurrencyLevel(4)
            .maximumSize(8)
            .expireAfterAccess(Duration.ofDays(1));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public GraphServiceImpl(GraphManager graphManager, CacheBuilder graphCacheBuilder) {
        this.graphManager = graphManager;
        this.graphCache = graphCacheBuilder
            .recordStats()
            .build(new GraphCacheLoader());
    }

    @Override
    public Graph getGraphByClassifier(GraphClassifier graphClassifier) {
        try {
            return this.graphCache.get(graphClassifier);
        } catch (ExecutionException e) {
            LOG.warn("Graph not found for: " + graphClassifier);
            return null;
        }
    }

    @Override
    public Graph getGraphById(String graphClassifierId) {
        GraphClassifier graphClassifier = this.graphManager.resolveGraphClassifier(graphClassifierId);
        return getGraphByClassifier(graphClassifier);
    }

    @Override
    public void putGraph(GraphClassifier graphClassifier, Graph graph) {
        this.graphCache.put(graphClassifier, graph);
    }

    @Override
    public CacheStats getGraphCacheStatitstics() {
        return this.graphCache.stats();
    }

    @Override
    public CacheStats getGraphClassifierCacheStatistics() {
        return this.graphManager.getCacheStatistics();
    }

    @Override
    public void evictCacheByGraphClassifier(GraphClassifier graphClassifier) {
        this.graphCache.invalidate(graphClassifier);
        this.graphManager.invalidate(graphClassifier);
    }

    @Override
    public void evictCache() {
        this.graphCache.invalidateAll();
        this.graphManager.evictCache();
    }

    // ========================================================================

    class GraphCacheLoader extends CacheLoader<GraphClassifier, Graph> {
        @Override
        public Graph load(GraphClassifier key) throws Exception {
            Graph graph = graphManager.buildGraphByClassifier(key);
            if (graph == null) {
                throw new ExecutionException("Graph can't be loaded for: " + key, new NullPointerException());
            }
            return  graph;
        }
    }

}
