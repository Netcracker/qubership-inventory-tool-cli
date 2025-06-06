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

import com.google.common.cache.CacheStats;

public interface GraphService {

    /** Get a Graph bound to given Classifier from cache or {@link GraphManager graphManager}
     *
     * @param graphClassifier Classifier
     * @return Graph, or {@code null}
     */
    Graph getGraphByClassifier(GraphClassifier graphClassifier);

    Graph getGraphById(String graphClassifierId);

    CacheStats getGraphCacheStatitstics();

    CacheStats getGraphClassifierCacheStatistics();

    /* Put a new Graph with given Classifier into the service.
     * If another graph instance with the same Classifier id exists, it will be replaced.
     */
    void putGraph(GraphClassifier graphClassifier, Graph graph);

    /* Remove a graph with given classifier from local cache.
     */
    void evictCacheByGraphClassifier(GraphClassifier graphClassifier);

    void evictCache();

}
