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

import java.util.List;

import org.qubership.itool.modules.graph.Graph;
import org.qubership.itool.modules.graph.GraphImpl;
import org.qubership.itool.modules.gremlin2.P;
import org.junit.jupiter.api.*;
import io.vertx.core.json.JsonObject;

import static org.qubership.itool.modules.graph.Graph.F_ID;
import static org.qubership.itool.modules.graph.Graph.F_NAME;
import static org.qubership.itool.modules.graph.Graph.V_ROOT;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestGraph {

  private Graph graph;

  @BeforeAll
  public void setup() {
    this.graph = new GraphImpl();
  }

  @BeforeEach
  public void cleanup() {
    this.graph.clear();
  }

  @Test
  void testEmptyGraphGetRoot() {
    JsonObject root = this.graph.getVertex(Graph.V_ROOT);

    this.graph.printGraph();
    Assertions.assertNotNull(root);
    Assertions.assertEquals(Graph.V_ROOT, root.getString(F_ID));
    Assertions.assertEquals(1, this.graph.getVertexCount());
  }

  @Test
  void testEmptyGraphRootGetVertex() {
    JsonObject root = this.graph.getVertex(Graph.V_ROOT);

    this.graph.printGraph();
    Assertions.assertNotNull(root);
    Assertions.assertEquals(Graph.V_ROOT, root.getString(F_ID));
    Assertions.assertEquals(Graph.V_ROOT, root.getString(Graph.F_NAME));
  }

  @Test
  void testAddOneVertextUnderRoot() {
    JsonObject json = new JsonObject().put(F_ID, "vertex_1").put(Graph.F_NAME, "name_1");

    boolean result = this.graph.addVertexUnderRoot(json);
    Assertions.assertTrue(result);

    JsonObject vertex1 = this.graph.getVertex("vertex_1");

    this.graph.printGraph();
    Assertions.assertNotNull(vertex1);
    Assertions.assertEquals("vertex_1", vertex1.getString(F_ID));
    Assertions.assertEquals("name_1", vertex1.getString(Graph.F_NAME));
    Assertions.assertEquals(2, this.graph.getVertexCount());
  }

  @Test
  void testAddTwoVertextUnderRoot() {
    JsonObject json1 = new JsonObject().put(F_ID, "vertex_1").put(Graph.F_NAME, "name_1");
    JsonObject json2 = new JsonObject().put(F_ID, "vertex_2").put(Graph.F_NAME, "name_2");

    boolean result1 = this.graph.addVertexUnderRoot(json1);
    Assertions.assertTrue(result1);

    boolean result2 = this.graph.addVertexUnderRoot(json2);
    Assertions.assertTrue(result2);

    JsonObject vertex1 = this.graph.getVertex("vertex_1");
    JsonObject vertex2 = this.graph.getVertex("vertex_2");

    this.graph.printGraph();
    Assertions.assertNotNull(vertex1);
    Assertions.assertEquals("vertex_1", vertex1.getString(F_ID));
    Assertions.assertEquals("name_1", vertex1.getString(Graph.F_NAME));
    Assertions.assertNotNull(vertex2);
    Assertions.assertEquals("vertex_2", vertex2.getString(F_ID));
    Assertions.assertEquals("name_2", vertex2.getString(Graph.F_NAME));
    Assertions.assertEquals(3, this.graph.getVertexCount());
  }

  @Test
  void testAddTwoVertextUderRootSameId() {
    JsonObject json1 = new JsonObject().put(F_ID, "vertex_1").put(Graph.F_NAME, "name_1");
    JsonObject json2 = new JsonObject().put(F_ID, "vertex_1").put(Graph.F_NAME, "name_2");

    boolean result1 = this.graph.addVertexUnderRoot(json1);
    Assertions.assertTrue(result1);

    boolean result2 = this.graph.addVertexUnderRoot(json2);
    Assertions.assertFalse(result2);

    this.graph.printGraph();
  }

  @Test
  void testAddVertexByJsonObject() {
    JsonObject json1 = new JsonObject().put(F_ID, "vertex_1").put(Graph.F_NAME, "name_1");
    JsonObject json1_1 = new JsonObject().put(F_ID, "vertex_1_1").put(Graph.F_NAME, "name_1_1");
    boolean result1 = this.graph.addVertexUnderRoot(json1);
    Assertions.assertTrue(result1);

    boolean result2 = this.graph.addVertex(json1, json1_1);
    Assertions.assertTrue(result2);

    JsonObject vertex1 = this.graph.getVertex("vertex_1");
    JsonObject vertex1_1 = this.graph.getVertex("vertex_1_1");

    this.graph.printGraph();
    Assertions.assertNotNull(vertex1);
    Assertions.assertEquals("vertex_1", vertex1.getString(F_ID));
    Assertions.assertEquals("name_1", vertex1.getString(Graph.F_NAME));
    Assertions.assertNotNull(vertex1_1);
    Assertions.assertEquals("vertex_1_1", vertex1_1.getString(F_ID));
    Assertions.assertEquals("name_1_1", vertex1_1.getString(Graph.F_NAME));
    Assertions.assertEquals(3, this.graph.getVertexCount());
  }

  @Test
  void testAddVertexById() {
    JsonObject json1 = new JsonObject().put(F_ID, "vertex_1").put(Graph.F_NAME, "name_1");
    JsonObject json1_1 = new JsonObject().put(F_ID, "vertex_1_1").put(Graph.F_NAME, "name_1_1");
    boolean result1 = this.graph.addVertexUnderRoot(json1);
    Assertions.assertTrue(result1);

    boolean result2 = this.graph.addVertex(json1.getString(F_ID), json1_1);
    Assertions.assertTrue(result2);

    JsonObject vertex1 = this.graph.getVertex("vertex_1");
    JsonObject vertex1_1 = this.graph.getVertex("vertex_1_1");

    this.graph.printGraph();
    Assertions.assertNotNull(vertex1);
    Assertions.assertEquals("vertex_1", vertex1.getString(F_ID));
    Assertions.assertEquals("name_1", vertex1.getString(Graph.F_NAME));
    Assertions.assertNotNull(vertex1_1);
    Assertions.assertEquals("vertex_1_1", vertex1_1.getString(F_ID));
    Assertions.assertEquals("name_1_1", vertex1_1.getString(Graph.F_NAME));
    Assertions.assertEquals(3, this.graph.getVertexCount());
  }

  @Test
  void testGetRootSuccessors() {
    JsonObject json1 = new JsonObject().put(F_ID, "vertex_1").put(Graph.F_NAME, "name_1");
    JsonObject json1_1 = new JsonObject().put(F_ID, "vertex_1_1").put(Graph.F_NAME, "name_1_1");
    boolean result1 = this.graph.addVertexUnderRoot(json1);
    Assertions.assertTrue(result1);

    boolean result2 = this.graph.addVertex(json1, json1_1);
    Assertions.assertTrue(result2);

    this.graph.printGraph();

    List<JsonObject> childs = this.graph.getRootSuccessors();
    Assertions.assertEquals(3, this.graph.getVertexCount());
    Assertions.assertEquals(1, childs.size());

    JsonObject child = childs.get(0);
    Assertions.assertNotNull(child);
    Assertions.assertEquals("vertex_1", child.getString(F_ID));
    Assertions.assertEquals("name_1", child.getString(Graph.F_NAME));
  }

  @Test
  void testGetSuccessors() {
    JsonObject json1 = new JsonObject().put(F_ID, "vertex_1").put(Graph.F_NAME, "name_1");
    JsonObject json1_1 = new JsonObject().put(F_ID, "vertex_1_1").put(Graph.F_NAME, "name_1_1");
    boolean result1 = this.graph.addVertexUnderRoot(json1);
    Assertions.assertTrue(result1);

    boolean result2 = this.graph.addVertex(json1, json1_1);
    Assertions.assertTrue(result2);

    this.graph.printGraph();

    List<JsonObject> childs = this.graph.getSuccessors(json1.getString(F_ID), false);
    Assertions.assertEquals(3, this.graph.getVertexCount());
    Assertions.assertEquals(1, childs.size());

    JsonObject child = childs.get(0);
    Assertions.assertNotNull(child);
    Assertions.assertEquals("vertex_1_1", child.getString(F_ID));
    Assertions.assertEquals("name_1_1", child.getString(Graph.F_NAME));
  }

  @Test
  void testGetSuccessorsById() {
    JsonObject json1 = new JsonObject().put(F_ID, "vertex_1").put(Graph.F_NAME, "name_1");
    JsonObject json1_1 = new JsonObject().put(F_ID, "vertex_1_1").put(Graph.F_NAME, "name_1_1");
    boolean result1 = this.graph.addVertexUnderRoot(json1);
    Assertions.assertTrue(result1);

    boolean result2 = this.graph.addVertex(json1, json1_1);
    Assertions.assertTrue(result2);

    this.graph.printGraph();

    List<JsonObject> childs = this.graph.getSuccessors(json1.getString(F_ID), false);
    Assertions.assertEquals(3, this.graph.getVertexCount());
    Assertions.assertEquals(1, childs.size());

    JsonObject child = childs.get(0);
    Assertions.assertNotNull(child);
    Assertions.assertEquals("vertex_1_1", child.getString(F_ID));
    Assertions.assertEquals("name_1_1", child.getString(Graph.F_NAME));
  }


  @Test
  void testGetPredecessors() {
    JsonObject json1 = new JsonObject().put(F_ID, "vertex_1").put(Graph.F_NAME, "name_1");
    JsonObject json1_1 = new JsonObject().put(F_ID, "vertex_1_1").put(Graph.F_NAME, "name_1_1");
    boolean result1 = this.graph.addVertexUnderRoot(json1);
    Assertions.assertTrue(result1);

    boolean result2 = this.graph.addVertex(json1, json1_1);
    Assertions.assertTrue(result2);

    this.graph.printGraph();

    List<JsonObject> parents = this.graph.getPredecessors(json1_1.getString(F_ID), false);
    Assertions.assertEquals(3, this.graph.getVertexCount());
    Assertions.assertEquals(1, parents.size());

    JsonObject parent = parents.get(0);
    Assertions.assertNotNull(parent);
    Assertions.assertEquals("vertex_1", parent.getString(F_ID));
    Assertions.assertEquals("name_1", parent.getString(Graph.F_NAME));
  }

  @Test
  void testGetPredecessorsById() {
    JsonObject json1 = new JsonObject().put(F_ID, "vertex_1").put(Graph.F_NAME, "name_1");
    JsonObject json1_1 = new JsonObject().put(F_ID, "vertex_1_1").put(Graph.F_NAME, "name_1_1");
    boolean result1 = this.graph.addVertexUnderRoot(json1);
    Assertions.assertTrue(result1);

    boolean result2 = this.graph.addVertex(json1, json1_1);
    Assertions.assertTrue(result2);

    this.graph.printGraph();

    List<JsonObject> parents = this.graph.getPredecessors(json1_1.getString(F_ID), false);
    Assertions.assertEquals(3, this.graph.getVertexCount());
    Assertions.assertEquals(1, parents.size());

    JsonObject parent = parents.get(0);
    Assertions.assertNotNull(parent);
    Assertions.assertEquals("vertex_1", parent.getString(F_ID));
    Assertions.assertEquals("name_1", parent.getString(Graph.F_NAME));
  }

    @Test
    void testAddEdge() {
        JsonObject json1 = new JsonObject().put(F_ID, "vertex_1").put(Graph.F_NAME, "name_1");
        JsonObject json1_1 = new JsonObject().put(F_ID, "vertex_1_1").put(Graph.F_NAME, "name_1_1");
        JsonObject json2 = new JsonObject().put(F_ID, "vertex_2").put(Graph.F_NAME, "name_2");

        boolean result1 = this.graph.addVertexUnderRoot(json1);
        Assertions.assertTrue(result1);

        boolean result2 = this.graph.addVertex(json1, json1_1);
        Assertions.assertTrue(result2);

        boolean result3 = this.graph.addVertexUnderRoot(json2);
        Assertions.assertTrue(result3);

        String result4 = this.graph.addEdge(json1_1, json2);
        Assertions.assertNotNull(result4);

        JsonObject edge1 = new JsonObject().put("key1", "value1");
        JsonObject edge2 = new JsonObject().put("key1", "value1").put("key2", "value2");
        JsonObject edge3 = new JsonObject().put("key2", "value2");
        JsonObject edge4 = new JsonObject().put("key2", "value2").put("key1", "value1");

        Assertions.assertNotNull(this.graph.addEdge(json1, json2, edge1));
        Assertions.assertNotNull(this.graph.addEdge(json1, json2, edge2));
        Assertions.assertNotNull(this.graph.addEdge(json1, json2, edge3));
        Assertions.assertNull(this.graph.addEdge(json1, json2, edge4));

        this.graph.printGraph();

        Assertions.assertEquals(4, this.graph.getVertexCount());
        Assertions.assertEquals(7, this.graph.getEdgeCount());
    }

    @Test
    void testCircularGraph() {
        JsonObject json1 = new JsonObject().put(F_ID, "vertex_1").put(Graph.F_NAME, "name_1");
        JsonObject json1_1 = new JsonObject().put(F_ID, "vertex_1_1").put(Graph.F_NAME, "name_1_1");
        JsonObject json2 = new JsonObject().put(F_ID, "vertex_2").put(Graph.F_NAME, "name_2");
        boolean result1 = this.graph.addVertexUnderRoot(json1);
        Assertions.assertTrue(result1);

        boolean result2 = this.graph.addVertex(json1, json1_1);
        Assertions.assertTrue(result2);

        boolean result3 = this.graph.addVertexUnderRoot(json2);
        Assertions.assertTrue(result3);

        JsonObject edge1 = new JsonObject().put("type", "database");
        String result4 = this.graph.addEdge(json1_1, json2, edge1);
        Assertions.assertNotNull(result4);

        JsonObject edge2 = new JsonObject().put("type", "http");
        String result5 = this.graph.addEdge(json2, json1_1, edge2);
        Assertions.assertNotNull(result5);

        this.graph.printGraph();

        Assertions.assertEquals(4, this.graph.getVertexCount());
    }

  @Test
  void testRelocateVertex() {
    JsonObject json1 = new JsonObject().put(F_ID, "vertex_1").put(Graph.F_NAME, "name_1");
    JsonObject json1_1 = new JsonObject().put(F_ID, "vertex_1_1").put(Graph.F_NAME, "name_1_1");
    JsonObject json2 = new JsonObject().put(F_ID, "vertex_2").put(Graph.F_NAME, "name_2");

    this.graph.addVertexUnderRoot(json1);
    this.graph.addVertex(json1, json1_1);
    this.graph.addVertexUnderRoot(json2);
    this.graph.addEdge(json1_1, json2);

    JsonObject edge1 = new JsonObject().put("key1", "value1");
    JsonObject edge2 = new JsonObject().put("key1", "value1").put("key2", "value2");
    JsonObject edge3 = new JsonObject().put("key2", "value2");

    this.graph.addEdge(json1, json2, edge1);
    this.graph.addEdge(json1, json2, edge2);
    this.graph.addEdge(json1, json2, edge3);

    var vertex_1_1 = this.graph.traversal().V(V_ROOT).out().out().has(F_NAME, P.eq("name_1_1")).dedup().next();
    Assertions.assertEquals("vertex_1_1", vertex_1_1.getString(F_ID));
    var vertex_2 = this.graph.traversal().V(V_ROOT).out().out().has(F_NAME, P.eq("name_1_1")).out().next();
    Assertions.assertNotNull(vertex_2);
    Assertions.assertEquals("vertex_2", vertex_2.getString(F_ID));

    this.graph.relocateVertex(json1_1, "vertex_relocated");
    var vertex_relocated = this.graph.traversal().V(V_ROOT).out().out().has(F_NAME, P.eq("name_1_1")).next();
    Assertions.assertEquals("vertex_relocated", vertex_relocated.getString(F_ID));
    vertex_2 = this.graph.traversal().V(V_ROOT).out().out().has(F_NAME, P.eq("name_1_1")).out().next();
    Assertions.assertNotNull(vertex_2);
    Assertions.assertEquals("vertex_2", vertex_2.getString(F_ID));

    this.graph.printGraph();

    Assertions.assertEquals(4, this.graph.getVertexCount());
    Assertions.assertEquals(7, this.graph.getEdgeCount());
  }
}
