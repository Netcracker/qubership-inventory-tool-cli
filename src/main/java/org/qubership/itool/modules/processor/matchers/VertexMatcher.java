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

package org.qubership.itool.modules.processor.matchers;

import org.qubership.itool.modules.graph.Graph;

import io.vertx.core.json.JsonObject;

public interface VertexMatcher {

    /** Try to find existing vertex in the target graph that matches the new vertex from the source graph.
     *
     * @param sourceGraph A graph that is being merged into the target graph
     * @param newVertex A new vertex from the source graph that is about to be merged into the target graph
     * @param targetGraph A graph to accept the new vertex
     * @return Matching vertex in target graph or {@code null}
     */
    JsonObject findExistingVertex(Graph sourceGraph, JsonObject newVertex, Graph targetGraph);

}
