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

import org.qubership.itool.modules.gremlin2.graph.GraphTraversalSource;
import io.vertx.core.json.JsonObject;

import java.util.List;

public interface BasicGraph {

    GraphTraversalSource traversal();

    int getVertexCount();

    int getEdgeCount();

    List<JsonObject> edgeList();

    List<JsonObject> vertexList();

    JsonObject getVertex(String vertexId);

    JsonObject getEdge(String edgeId);

    List<JsonObject> getSuccessors(String vertexId, boolean distinct);

    List<JsonObject> getPredecessors(String vertexId, boolean distinct);

    List<JsonObject> getSuccessorEdges(String vertexId);

    List<JsonObject> getPredecessorEdges(String vertexId);

    List<JsonObject> getEdgesBetween(String vertexFromId, String vertexToId);

    List<JsonObject> getEdgesBetween(JsonObject vertexFrom, JsonObject vertexTo);

    JsonObject getEdgeTarget(String edgeId);

    JsonObject getEdgeSource(String edgeId);

}
