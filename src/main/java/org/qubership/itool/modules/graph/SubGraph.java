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

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.qubership.itool.modules.graph.Graph.F_ID;

// XXX All the methods of this class should synchronize on origGraph, yet they are read-only
public class SubGraph implements BasicGraph {

    private BasicGraph origGraph;
    private Map<String, JsonObject> edges;
    private Map<String, JsonObject> vertices;

    @Override
    public GraphTraversalSource traversal() {
        return new GraphTraversalSource(this);
    }

    public SubGraph(BasicGraph graph, List<JsonObject> edgeList) {
        this.origGraph = graph;
        this.edges = edgeList.stream().collect(Collectors.toMap(edge -> edge.getString(F_ID), Function.identity()));

        this.vertices = new LinkedHashMap<>();
        for (JsonObject edge: edgeList) {
            String edgeId = edge.getString(F_ID);
            JsonObject edgeSource = graph.getEdgeSource(edgeId);
            this.vertices.put(edgeSource.getString(F_ID), edgeSource);
            JsonObject edgeTarget = graph.getEdgeTarget(edgeId);
            this.vertices.put(edgeTarget.getString(F_ID), edgeTarget);
        }
    }

    /* Add an edge from the original graph into this subgraph */
    public void addEdge(String edgeId) {
        JsonObject edge = getEdge(edgeId);
        if (edge != null) {
            return;
        }
        edge = origGraph.getEdge(edgeId);
        edges.put(edgeId, edge);

        JsonObject vertexSource = origGraph.getEdgeSource(edge.getString(F_ID));
        String sourceId = vertexSource.getString(F_ID);
        if (getVertex(sourceId) == null) {
            vertices.put(sourceId, vertexSource);
        }

        JsonObject vertexTarget = origGraph.getEdgeTarget(edge.getString(F_ID));
        String targetId = vertexTarget.getString(F_ID);
        if (getVertex(targetId) == null) {
            vertices.put(targetId, vertexTarget);
        }
    }

    @Override
    public int getVertexCount() {
        return vertices.size();
    }

    @Override
    public int getEdgeCount() {
        return edges.size();
    }

    @Override
    public List<JsonObject> edgeList() {
        return new ArrayList<>(edges.values());
    }

    @Override
    public List<JsonObject> vertexList() {
        return new ArrayList<>(vertices.values());
    }

    @Override
    public JsonObject getVertex(String vertexId) {
        return vertices.get(vertexId);
    }

    @Override
    public JsonObject getEdge(String edgeId) {
        return edges.get(edgeId);
    }

    @Override
    public List<JsonObject> getSuccessors(String vertexId, boolean distinct) {
        JsonObject vertex = getVertex(vertexId);
        if (vertex == null) {
            return null;
        }
        List<JsonObject> successors = origGraph.getSuccessorEdges(vertexId).stream()
            .filter(edge -> getEdge(edge.getString(F_ID)) != null)
            .map(edge -> origGraph.getEdgeTarget(edge.getString(F_ID)))
            .collect(Collectors.toList());
        if (distinct) {
            Set<JsonObject> unique = Collections.newSetFromMap(new IdentityHashMap<>());
            successors.removeIf(v -> ! unique.add(v));
        }
        return successors;
    }

    @Override
    public List<JsonObject> getPredecessors(String vertexId, boolean distinct) {
        JsonObject vertex = getVertex(vertexId);
        if (vertex == null) {
            return null;
        }
        List<JsonObject> predecessors = origGraph.getPredecessorEdges(vertexId).stream()
            .filter(edge -> getEdge(edge.getString(F_ID)) != null)
            .map(edge -> origGraph.getEdgeSource(edge.getString(F_ID)))
            .collect(Collectors.toList());
        if (distinct) {
            Set<JsonObject> unique = Collections.newSetFromMap(new IdentityHashMap<>());
            predecessors.removeIf(v -> ! unique.add(v));
        }
        return predecessors;
    }

    @Override
    public List<JsonObject> getSuccessorEdges(String vertexId) {
        JsonObject vertex = getVertex(vertexId);
        if (vertex == null) {
            return null;
        }
        List<JsonObject> list = origGraph.getSuccessorEdges(vertexId);
        List<JsonObject> result = new ArrayList<>();
        for (JsonObject item : list) {
            if (getEdge(item.getString(F_ID)) != null) {
                result.add(item);
            }
        }
        return result;
    }

    @Override
    public List<JsonObject> getPredecessorEdges(String vertexId) {
        JsonObject vertex = getVertex(vertexId);
        if (vertex == null) {
            return null;
        }
        List<JsonObject> list = origGraph.getPredecessorEdges(vertexId);
        List<JsonObject> result = new ArrayList<>();
        for (JsonObject item : list) {
            if (getEdge(item.getString(F_ID)) != null) {
                result.add(item);
            }
        }
        return result;
    }

    @Override
    public List<JsonObject> getEdgesBetween(String vertexFromId, String vertexToId) {
        if (! vertices.containsKey(vertexFromId) || ! vertices.containsKey(vertexToId) ) {
            return Collections.emptyList();
        }
        return origGraph.getEdgesBetween(vertexFromId, vertexToId)
                .stream()
                .filter(edge -> getEdge(edge.getString(F_ID)) != null)
                .collect(Collectors.toList());
    }

    @Override
    public List<JsonObject> getEdgesBetween(JsonObject vertexFrom, JsonObject vertexTo) {
        return getEdgesBetween(vertexFrom.getString(F_ID), vertexTo.getString(F_ID));
    }


    @Override
    public JsonObject getEdgeTarget(String edgeId) {
        JsonObject edge = getEdge(edgeId);
        if (edge == null) {
            return null;
        }
        JsonObject vertex = origGraph.getEdgeTarget(edgeId);
        return  (vertex != null && getVertex(vertex.getString(F_ID)) != null) ? vertex : null;
    }

    @Override
    public JsonObject getEdgeSource(String edgeId) {
        JsonObject edge = getEdge(edgeId);
        if (edge == null) {
            return null;
        }
        JsonObject vertex = origGraph.getEdgeSource(edgeId);
        return  (vertex != null && getVertex(vertex.getString(F_ID)) != null) ? vertex : null;
    }

}
