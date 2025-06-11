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
import org.qubership.itool.modules.processor.InvalidGraphException;
import org.qubership.itool.modules.report.GraphReport;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class GraphImpl implements Graph {

    private static final Logger LOG = LoggerFactory.getLogger(GraphImpl.class);

    private int graphVersion;
    private Map<String, Vertex> vertices;
    private Map<String, Edge> edges;

    private int edgeGeneratorCounter;

    private GraphReport report;

    public GraphImpl() {
        clear();
        graphVersion = CURRENT_GRAPH_MODEL_VERSION;
    }


    @Override
    public GraphTraversalSource traversal() {
        return new GraphTraversalSource(this);
    }

    @Override
    public synchronized JsonObject getVertex(String vertexId) {
        if (!this.vertices.containsKey(vertexId)) {
            return null;
        }
        return this.vertices.get(vertexId).getValue();
    }

    @Override
    public synchronized boolean addVertex(JsonObject vertex) {
        String vertexId = vertex.getString(F_ID);
        if (vertexId == null || this.vertices.containsKey(vertexId)) {
            return false;
        }

        Vertex vertexObj = new VertexImpl(vertexId, vertex);
        this.vertices.put(vertexId, vertexObj);
        return true;
    }

    @Override
    public synchronized boolean addVertex(String sourceVertexId, JsonObject destinationVertex) {
        JsonObject sourceVertex = getVertex(sourceVertexId);

        if (sourceVertex == null) {
            return false;
        }

        return addVertex(sourceVertex, destinationVertex);
    }

    @Override
    public synchronized boolean addVertex(JsonObject sourceVertex, JsonObject destinationVertex) {
        String srcVertexId = sourceVertex.getString(F_ID);
        String destVertexId = destinationVertex.getString(F_ID);

        if (srcVertexId == null || destVertexId == null
            || !this.vertices.containsKey(srcVertexId)
            || this.vertices.containsKey(destVertexId)) {
            return false;
        }

        // Add related vertex and edge
        addVertex(destinationVertex);
        addEdge(sourceVertex, destinationVertex, null);

        return true;
    }

    @Override
    public synchronized boolean addVertexUnderRoot(JsonObject vertex) {
        String vertexId = vertex.getString(F_ID);
        if (vertexId == null || this.vertices.containsKey(vertexId)) {
            return false;
        }

        addVertex(vertex);
        addEdge(getVertex(V_ROOT), vertex, null);

        return true;
    }

    @Override
    public synchronized boolean relocateVertex(JsonObject vertex, String newId) {
        String oldId = vertex.getString(F_ID);
        if (   oldId == null
            || ! vertices.containsKey(oldId)
            || vertices.containsKey(newId))   // New id already exists, or relocation to the same id requested
        {
            return false;
        }

        LOG.debug("Relocating vertex {} to {}", oldId, newId);
        Vertex vertexObj = moveVertexToNewId(vertex, newId, oldId);
        for (String outgoingEdgeId: vertexObj.getEdgesOut()) {
            Edge outgoingEdge = this.edges.get(outgoingEdgeId);
            if (outgoingEdge != null) {
                LOG.debug(" - Edge {} changed its source", outgoingEdgeId);
                outgoingEdge.setSourceVertexId(newId);
            }
        }
        for (String incomingEdgeId: vertexObj.getEdgesIn()) {
            Edge incomingEdge = this.edges.get(incomingEdgeId);
            if (incomingEdge != null) {
                LOG.debug(" - Edge {} changed its destination", incomingEdgeId);
                incomingEdge.setDestinationVertexId(newId);
            }
        }

        return true;
    }

    private Vertex moveVertexToNewId(JsonObject vertex, String newId, String oldId) {
        Vertex oldVertexObj = this.vertices.get(oldId);
        vertices.remove(oldId);
        vertex.put(F_ID, newId);
        this.addVertex(vertex);
        Vertex newVertexObj = this.vertices.get(newId);
        newVertexObj.getEdgesIn().addAll(oldVertexObj.getEdgesIn());
        newVertexObj.getEdgesOut().addAll(oldVertexObj.getEdgesOut());
        return oldVertexObj;
    }


    @Override
    public synchronized boolean removeVertex(JsonObject vertex) {
        String vertexId = vertex.getString(F_ID);
        if (vertexId == null || !this.vertices.containsKey(vertexId)) {
            return false;
        }

        Vertex vertexObj = this.vertices.get(vertexId);

        for (String outgoingEdgeId: vertexObj.getEdgesOut()) {
            Edge outgoingEdge = this.edges.get(outgoingEdgeId);
            if (outgoingEdge != null) {
                String destinationVertexId = outgoingEdge.getDestinationVertexId();
                Vertex destinationVertex = this.vertices.get(destinationVertexId);
                destinationVertex.getEdgesIn().remove(outgoingEdgeId);
                this.edges.remove(outgoingEdgeId);
            }
        }

        for (String incomingEdgeId: vertexObj.getEdgesIn()) {
            Edge incomingEdge = this.edges.get(incomingEdgeId);
            if (incomingEdge != null) {
                String edgeFrom = incomingEdge.getSourceVertexId();
                Vertex edgeSource = this.vertices.get(edgeFrom);
                edgeSource.getEdgesOut().remove(incomingEdgeId);
                this.edges.remove(incomingEdgeId);
            }
        }

        return this.vertices.remove(vertexId) != null;
    }

    @Override
    public synchronized JsonObject getEdge(String edgeId) {
        if (!this.edges.containsKey(edgeId)) {
            return null;
        }
        return this.edges.get(edgeId).getValue();
    }

    @Override
    public synchronized String addEdge(JsonObject sourceVertex, JsonObject destinationVertex) {
        return addEdge(sourceVertex, destinationVertex, null);
    }

    @Override
    public synchronized String addEdge(JsonObject sourceVertex, JsonObject destinationVertex, JsonObject edge) {

        if (edge == null) {
            edge = new JsonObject();
            edge.put(F_ID, generateEdgeId());
        } else {
            // return null if edge with provided id (maybe, exactly this one) already exists in the graph
            if (this.edges.containsKey(edge.getString(F_ID))) {
                return null;
            }
        }

        String edgeId = edge.getString(F_ID);
        if (edgeId == null) {
            edgeId = generateEdgeId();
            edge.put(F_ID, edgeId);
        }

        // Add vertex if not exist
        if (!this.vertices.containsKey(sourceVertex.getString(F_ID))) {
            addVertex(sourceVertex);
        }

        if (!this.vertices.containsKey(destinationVertex.getString(F_ID))) {
            addVertex(destinationVertex);
        }

        Vertex source = this.vertices.get(sourceVertex.getString(F_ID));
        Vertex target = this.vertices.get(destinationVertex.getString(F_ID));
        Set<String> allEdges = getEdgeIdsBetween(source, target);

        // return false if edgeValue matches with existingEdge
        if (!allEdges.isEmpty()) {
            Map<String, Object> edgeMapWithoutID = asMapWithoutID(edge);
            for (String id : allEdges) {
                JsonObject existingEdge = getEdge(id);
                if (edgeMapWithoutID.equals(asMapWithoutID(existingEdge))) {
                    return null;
                }
            }
        }

        EdgeImpl edgeObj = new EdgeImpl(edge.getString(F_ID), sourceVertex.getString(F_ID),
            destinationVertex.getString(F_ID), edge);
        this.edges.put(edge.getString(F_ID), edgeObj);
        source.getEdgesOut().add(edgeObj.getId());
        target.getEdgesIn().add(edgeObj.getId());

        // Return generated (or provided) edgeId
        return edgeId;
    }

    private String generateEdgeId() {
        String edgeId;
        do {
            edgeId = "edge::" + edgeGeneratorCounter;
            edgeGeneratorCounter++;
        } while (edges.containsKey(edgeId));
        return edgeId;
    }

    @Override
    public synchronized int removeAllEdges(JsonObject sourceVertex, JsonObject destinationVertex) {
        String srcVertexId = sourceVertex.getString(F_ID);
        String destVertexId = destinationVertex.getString(F_ID);

        if (srcVertexId == null || destVertexId == null
            || !this.vertices.containsKey(srcVertexId)
            || !this.vertices.containsKey(destVertexId)) {
            return 0;
        }

        Vertex edgeSource = this.vertices.get(sourceVertex.getString(F_ID));
        Vertex edgeTarget = this.vertices.get(destinationVertex.getString(F_ID));

        Set<String> allEdges = getEdgeIdsBetween(edgeSource, edgeTarget);
        this.edges.keySet().removeAll(allEdges);
        edgeSource.getEdgesOut().removeAll(allEdges);
        edgeTarget.getEdgesIn().removeAll(allEdges);

        return allEdges.size();
    }

    @Override
    public synchronized List<JsonObject> vertexList() {
        return this.vertices.values().stream().map(Vertex::getValue).collect(Collectors.toList());
    }

    @Override
    public synchronized List<JsonObject> edgeList() {
        return this.edges.values().stream().map(Edge::getValue).collect(Collectors.toList());
    }

    @Override
    public synchronized List<JsonObject> getRootSuccessors() {
        return getSuccessors(V_ROOT, true);
    }

    @Override
    public synchronized List<JsonObject> getSuccessors(String vertexId, boolean distinct) {
        Vertex vertex = this.vertices.get(vertexId);
        List<JsonObject> successors = vertex.getEdgesOut()
            .stream()
            .map(edgeId -> this.vertices.get(this.edges.get(edgeId).getDestinationVertexId()))
            .map(Vertex::getValue)
            .collect(Collectors.toList());
        if (distinct) {
            Set<JsonObject> unique = Collections.newSetFromMap(new IdentityHashMap<>());
            successors.removeIf(v -> ! unique.add(v));
        }
        return successors;
    }

    @Override
    public synchronized List<JsonObject> getPredecessors(String vertexId, boolean distinct) {
        Vertex vertex = this.vertices.get(vertexId);
        List<JsonObject> predecessors = vertex.getEdgesIn()
            .stream()
            .map(edgeId -> this.vertices.get(this.edges.get(edgeId).getSourceVertexId()))
            .map(Vertex::getValue)
            .collect(Collectors.toList());
        if (distinct) {
            Set<JsonObject> unique = Collections.newSetFromMap(new IdentityHashMap<>());
            predecessors.removeIf(v -> ! unique.add(v));
        }
        return predecessors;
    }

    @Override
    public synchronized List<JsonObject> getSuccessorEdges(String vertexId) {
        Vertex vertex = this.vertices.get(vertexId);
        return vertex.getEdgesOut()
            .stream()
            .map(edgeId -> this.edges.get(edgeId))
            .map(Edge::getValue)
            .collect(Collectors.toList());
    }

    @Override
    public synchronized List<JsonObject> getPredecessorEdges(String vertexId) {
        Vertex vertex = this.vertices.get(vertexId);
        return vertex.getEdgesIn()
            .stream()
            .map(edgeId -> this.edges.get(edgeId))
            .map(Edge::getValue)
            .collect(Collectors.toList());
    }

    @Override
    public synchronized List<JsonObject> getEdgesBetween(String vertexFromId, String vertexToId) {
        Vertex vertexFrom = this.vertices.get(vertexFromId);
        return vertexFrom.getEdgesOut()
            .stream()
            .map(edgeId -> this.edges.get(edgeId))
            .filter(edge -> edge.getDestinationVertexId().equals(vertexToId))
            .map(Edge::getValue)
            .collect(Collectors.toList());
    }

    @Override
    public synchronized List<JsonObject> getEdgesBetween(JsonObject vertexFrom, JsonObject vertexTo) {
        return getEdgesBetween(vertexFrom.getString(F_ID), vertexTo.getString(F_ID));
    }

    private Set<String> getEdgeIdsBetween(Vertex sourceVertex, Vertex destinationVertex) {
        return sourceVertex.getEdgesOut().stream()
            .filter(destinationVertex.getEdgesIn()::contains)
            .collect(Collectors.toSet());
    }

    @Override
    public synchronized JsonObject getEdgeTarget(String edgeId) {
        Edge edge = this.edges.get(edgeId);
        return this.vertices.get(edge.getDestinationVertexId()).getValue();
    }

    @Override
    public synchronized JsonObject getEdgeSource(String edgeId) {
        Edge edge = this.edges.get(edgeId);
        return this.vertices.get(edge.getSourceVertexId()).getValue();
    }

    @Override
    public void clear() {
        this.vertices = new LinkedHashMap<>();
        this.edges = new LinkedHashMap<>();
        JsonObject rootVertex = new JsonObject()
            .put(F_ID, V_ROOT)
            .put(F_TYPE, V_ROOT)
            .put(F_NAME, V_ROOT);
        addVertex(rootVertex);
        this.edgeGeneratorCounter = 0;
    }

    @Override
    public synchronized JsonObject dumpGraphData(boolean deepCopy) {
        JsonObject result = new JsonObject();
        JsonObject sourceRoot = getVertex(V_ROOT);
        result.put("modelVersion", graphVersion);
        result.put("root", deepCopy ? sourceRoot.copy() : sourceRoot);
        result.put("edgeGeneratorCounter", edgeGeneratorCounter);
        JsonArray vertexArray = new JsonArray();
        result.put("vertexList", vertexArray);

        for (Vertex vertex : this.vertices.values()) {
            JsonObject value = vertex.getValue();
            if (V_ROOT.equals(value.getString(F_TYPE))) {
                continue;
            }
            vertexArray.add(deepCopy ? value.copy() : value);
        }

        JsonArray edgeArray = new JsonArray();
        result.put("edgeList", edgeArray);

        for (Edge edge : this.edges.values()) {
            JsonObject edgeResult = new JsonObject();
            edgeArray.add(edgeResult);

            edgeResult.put("source", edge.getSourceVertexId());
            edgeResult.put("target", edge.getDestinationVertexId());
            edgeResult.put("edge", deepCopy ? edge.getValue().copy() : edge.getValue());
        }

        return result;
    }

    /**
     * Restore graph structure from dump for known model versions
     * @param dump Graph data dump.
     */
    @Override
    public void restoreGraphData(JsonObject dump) {
        int modelVersion = dump.getInteger("modelVersion", FIRST_VERSIONED_GRAPH_MODEL_VERSION);
        if (modelVersion > CURRENT_GRAPH_MODEL_VERSION) {
            throw new IllegalArgumentException("Graph model version " + modelVersion + " not supported");
        }

        setGraphVersion(modelVersion);
        this.vertices = new LinkedHashMap<>();
        this.edges = new LinkedHashMap<>();
        this.edgeGeneratorCounter = dump.getInteger("edgeGeneratorCounter", 0);

        JsonObject rootObj = dump.getJsonObject("root");
        if (rootObj == null) {
            throw new InvalidGraphException(this, "Missing root");
        }
        addVertex(rootObj);

        JsonArray vertexList = dump.getJsonArray("vertexList");
        if (vertexList == null) {
            throw new InvalidGraphException(this, "Missing vertexList");
        }
        for (Object obj : vertexList) {
            JsonObject vertexJson = (JsonObject) obj;
            if (addVertex(vertexJson) == false) {
                throw new InvalidGraphException(this, "Invalid or duplicate vertex: " + vertexJson.getString(F_ID));
            }
        }

        JsonArray edgeList = dump.getJsonArray("edgeList");
        if (edgeList == null) {
            throw new InvalidGraphException(this, "Missing edgeList");
        }
        for (Object obj : edgeList) {
            JsonObject edgeJson = (JsonObject) obj;
            String sourceId = edgeJson.getString("source");
            String targetId = edgeJson.getString("target");
            JsonObject edge = edgeJson.getJsonObject("edge");
            if (edge == null) {
                throw new InvalidGraphException(this, "No edge object found");
            }

            Vertex sourceVertex = this.vertices.get(sourceId);
            if (sourceVertex == null) {
                throw new InvalidGraphException(this, "Invalid edge from non-existing vertex " + sourceId);
            }
            Vertex targetVertex = this.vertices.get(targetId);
            if (targetVertex == null) {
                throw new InvalidGraphException(this, "Invalid edge to non-existing vertex " + targetId);
            }

            if (addEdge(sourceVertex.getValue(), targetVertex.getValue(), edge) == null) {
                throw new InvalidGraphException(this, "Invalid or duplicate edge: " + edge.getString(F_ID));
            }
        }
    }

    @Override
    public synchronized int getVertexCount() {
        return this.vertices.size();
    }

    @Override
    public synchronized int getEdgeCount() {
        return this.edges.size();
    }

    @Override
    public synchronized void printGraph() {
        walkAndPrint(new HashSet<>(), this.vertices.get(V_ROOT), 0);
    }

    private void walkAndPrint(Set<Vertex> stack, Vertex vertexObj, int level) {
        stack.add(vertexObj);
        System.out.println("(" + level + ") " + vertexObj.getValue());
        Set<JsonObject> outgoingEdges = vertexObj.getEdgesOut()
            .stream()
            .map(edgeId -> this.edges.get(edgeId).getValue())
            .collect(Collectors.toSet());
        int childLevel = level + 1;
        for (JsonObject edge : outgoingEdges) {
            System.out.println(
                "\t".repeat(level + 1) +
                    "--> " + this.getEdgeTarget(edge.getString(F_ID)).getString("id")
                    + " // " + edge);
            if (stack.contains(this.vertices.get(this.getEdgeTarget(edge.getString(F_ID)).getString(F_ID)))) {
                System.out.println("\t".repeat(level + 1) + "^^^ circular reference");
                continue;
            }
            walkAndPrint(stack, this.vertices.get(this.getEdgeTarget(edge.getString(F_ID)).getString(F_ID)), childLevel);
        }
        stack.remove(vertexObj);
    }

    private Map<String, Object> asMapWithoutID(JsonObject src) {
        if (!src.containsKey(F_ID)) {
            return src.getMap();
        }
        Map<String, Object> result = new LinkedHashMap<>(src.getMap());
        result.remove(F_ID);
        return result;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("Graph{\n");
        buf.append("vertexList=[\n");
        for (String key : this.vertices.keySet()) {
            Vertex vertex = this.vertices.get(key);
            buf.append(" ").append(vertex).append("\n");
        }
        buf.append("],\nedgeList=[\n");
        for (String key : this.edges.keySet()) {
            Edge edge = this.edges.get(key);
            buf.append(" ").append(edge).append("\n");
        }
        buf.append("]}");
        return buf.toString();
    }

    public GraphReport getReport() {
        return report;
    }

    public void setReport(GraphReport report) {
        this.report = report;
    }

    @Override
    public int getGraphVersion() {
        return graphVersion;
    }

    @Override
    public void setGraphVersion(int graphVersion) {
        this.graphVersion = graphVersion;
    }
}
