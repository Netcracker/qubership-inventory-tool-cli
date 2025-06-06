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

package org.qubership.itool.modules.gremlin2;

import java.util.*;

import org.junit.jupiter.api.Test;

import org.qubership.itool.modules.graph.Graph;
import org.qubership.itool.modules.graph.SubGraph;
import org.qubership.itool.modules.gremlin2.graph.GraphTraversalSource;

import io.vertx.core.json.JsonObject;

import static org.junit.jupiter.api.Assertions.*;

public class TestSubgraph extends AbstractGremlinTest {

    @Test
    public void testSubgraphByGremlin() {
        SubGraph result = V().outE("created").subgraph("G").<SubGraph>cap("G").next();

        GraphTraversalSource tr = new GraphTraversalSource(result);

        assertEquals(2l, tr.E().count().next());
        assertEquals(3l, tr.V().count().next());
    }

    @Test
    public void testSubgraphByEdge() {
        JsonObject edge_5_adored = new JsonObject().put("id", "e5").put("type", "adored");
        addEdge(graph, "v4", "v6", edge_5_adored);

        SubGraph subgraph = new SubGraph(graph, List.of(edge_5_adored));

        // Edges between v4 and v6
        List<String> vertices = subgraph.traversal().V().id().toList();
        assertEquals(2, vertices.size());
        assertEquals(Set.of("v4", "v6"), new HashSet<>(vertices));
        assertEquals(2, graph.getEdgesBetween("v4", "v6").size());

        List<JsonObject> edgesBetween = subgraph.getEdgesBetween("v4", "v6");
        assertEquals(1, edgesBetween.size());
        assertEquals("e5", edgesBetween.get(0).getString(Graph.F_ID));

        // Successors and predecessors thru bundle
        assertEquals(4, graph.getSuccessorEdges("v4").size());
        assertEquals(4, graph.getSuccessors("v4", false).size());
        assertEquals(3, graph.getSuccessors("v4", true).size());
        assertEquals(1, subgraph.getSuccessorEdges("v4").size());
        assertEquals(1, subgraph.getSuccessors("v4", false).size());

        assertEquals(2, graph.getPredecessorEdges("v6").size());
        assertEquals(2, graph.getPredecessors("v6", false).size());
        assertEquals(1, graph.getPredecessors("v6", true).size());
        assertEquals(1, subgraph.getPredecessorEdges("v6").size());
        assertEquals(1, subgraph.getPredecessors("v6", false).size());

    }

    private String addEdge(Graph graph, String v1, String v2, JsonObject edgeObject) {
        return graph.addEdge(graph.getVertex(v1), graph.getVertex(v2), edgeObject);
    }

}
