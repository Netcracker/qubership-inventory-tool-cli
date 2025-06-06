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

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;

import org.qubership.itool.modules.graph.Graph;
import org.qubership.itool.modules.graph.GraphImpl;
import org.qubership.itool.modules.gremlin2.Path;
import org.qubership.itool.modules.gremlin2.graph.GraphTraversal;
import org.qubership.itool.modules.gremlin2.graph.GraphTraversalSource;

import java.util.List;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AbstractGremlinTest {

    protected Graph graph;
    protected GraphTraversalSource graphTraversalSource;

    @BeforeAll
    public void setup() {
        this.graph = new GraphImpl();
        this.graphTraversalSource = new GraphTraversalSource(this.graph);
    }

    @BeforeEach
    public void cleanup() {
        this.graph.clear();
        createGraph();
    }

    protected void createGraph() {
        createSimpleGraph();
    }

    protected GraphTraversal<JsonObject, JsonObject> V(final String... vertexIds) {
        return this.graphTraversalSource.V(vertexIds);
    }

    protected GraphTraversal<JsonObject, JsonObject> E(final String... edgeIds) {
        return this.graphTraversalSource.E(edgeIds);
    }

    protected void createSimpleGraph() {
        /*
        v1_marko -knows-> v4_josh -created->    v3_lop
                                  -created->    v5_ripple
                                  -maintained-> v6_linux
         */
        JsonObject vertex_1_marko = new JsonObject().put("id", "v1").put("name", "marko").put("age", 29).put("type", "person");
        JsonObject vertex_4_josh = new JsonObject().put("id", "v4").put("name", "josh").put("age", 32).put("type", "person");
        JsonObject vertex_3_lop = new JsonObject().put("id", "v3").put("name", "lop").put("lang", "java").put("type", "soft");
        JsonObject vertex_5_ripple = new JsonObject().put("id", "v5").put("name", "ripple").put("lang", "java").put("type", "soft");
        JsonObject vertex_6_linux = new JsonObject().put("id", "v6").put("name", "linux").put("type", "os").put("active", true);

        JsonObject json_1 = new JsonObject().put("document", "123456789").put("weight", 78);
        vertex_1_marko.put("details", json_1);

        JsonArray array = new JsonArray();
        array.add(new JsonObject().put("seq", "1"));
        array.add(new JsonObject().put("seq", "2"));

        vertex_1_marko.put("array", array);

        JsonArray array_2 = new JsonArray().add("first").add("second").add("fourth");

        vertex_3_lop.put("labels", array_2);

        JsonArray array_3 = new JsonArray().add("second").add("third");

        vertex_4_josh.put("labels", array_3);

        JsonObject edge_1_knows = new JsonObject().put("id", "e1").put("type", "knows").put("relation", "parent");
        JsonObject edge_2_created = new JsonObject().put("id", "e2").put("type", "created");
        JsonObject edge_3_created = new JsonObject().put("id", "e3").put("type", "created");
        JsonObject edge_4_maintained = new JsonObject().put("id", "e4").put("type", "maintained").put("yearFrom", "2000");

        this.graph.addVertexUnderRoot(vertex_1_marko);
        this.graph.addEdge(vertex_1_marko, vertex_4_josh, edge_1_knows);
        this.graph.addEdge(vertex_4_josh, vertex_3_lop, edge_2_created);
        this.graph.addEdge(vertex_4_josh, vertex_5_ripple, edge_3_created);
        this.graph.addEdge(vertex_4_josh, vertex_6_linux, edge_4_maintained);
    }

    /*
    https://tinkerpop.apache.org/docs/3.4.10/images/tinkerpop-modern.png
     */
    protected void createComplexGraph() {
        JsonObject vertex_1_marko = new JsonObject().put("id", "v1").put("name", "marko").put("age", 29).put("type", "person");
        JsonObject vertex_2_vadas = new JsonObject().put("id", "v2").put("name", "vadas").put("age", 27).put("type", "person");
        JsonObject vertex_3_lop = new JsonObject().put("id", "v3").put("name", "lop").put("lang", "java").put("type", "software");
        JsonObject vertex_4_josh = new JsonObject().put("id", "v4").put("name", "josh").put("age", 32).put("type", "person");
        JsonObject vertex_5_ripple = new JsonObject().put("id", "v5").put("name", "ripple").put("lang", "java").put("type", "software");
        JsonObject vertex_6_peter = new JsonObject().put("id", "v6").put("name", "peter").put("age", 35).put("type", "person");

        JsonObject edge_7_knows = new JsonObject().put("id", "e7").put("type", "knows").put("weight", 0.5);
        JsonObject edge_8_knows = new JsonObject().put("id", "e8").put("type", "knows").put("weight", 1.0);
        JsonObject edge_9_created = new JsonObject().put("id", "e9").put("type", "created").put("weight", 0.4);
        JsonObject edge_10_created = new JsonObject().put("id", "e10").put("type", "created").put("weight", 1.0);
        JsonObject edge_11_created = new JsonObject().put("id", "e11").put("type", "created").put("weight", 0.4);
        JsonObject edge_12_created = new JsonObject().put("id", "e12").put("type", "created").put("weight", 0.2);

        this.graph.addEdge(vertex_1_marko, vertex_2_vadas, edge_7_knows);
        this.graph.addEdge(vertex_1_marko, vertex_4_josh, edge_8_knows);
        this.graph.addEdge(vertex_1_marko, vertex_3_lop, edge_9_created);
        this.graph.addEdge(vertex_4_josh, vertex_5_ripple, edge_10_created);
        this.graph.addEdge(vertex_4_josh, vertex_3_lop, edge_11_created);
        this.graph.addEdge(vertex_6_peter, vertex_3_lop, edge_12_created);
    }

    protected void createOneVertexTwoPathGraph() {
        JsonObject infra = new JsonObject().put("id", "Infra").put("type", "infra");
        JsonObject pg = new JsonObject().put("id", "pg").put("type", "database");
        JsonObject cassandra = new JsonObject().put("id", "cassandra").put("type", "database");
        JsonObject domain = new JsonObject().put("id", "d1").put("type", "domain");
        JsonObject component = new JsonObject().put("id", "c1").put("type", "backend")
            .put("details", new JsonObject().put("domain", "dm1"));

        this.graph.addVertexUnderRoot(infra);
        this.graph.addVertex(infra, pg);
        this.graph.addVertex(infra, cassandra);
        this.graph.addVertexUnderRoot(domain);
        this.graph.addVertex(domain, component);
        this.graph.addEdge(component, pg);
        this.graph.addEdge(component, cassandra);
    }

    protected void createLoopedGraph() {
        JsonObject domain1 = createVertex("DOMAIN1", "domain");
        JsonObject domain1Library1 = createVertex("DOMAIN1-LIB", "library");
        JsonObject domain1Library1Module1 = createVertex("DOMAIN1-LIB-M1", "library");
        JsonObject domain1Library1Module2 = createVertex("DOMAIN1-LIB-M2", "library");
        this.graph.addVertexUnderRoot(domain1);
        this.graph.addVertex(domain1, domain1Library1);
        createRelation("e100", "module", domain1Library1, domain1Library1Module1);
        createRelation("e101", "module", domain1Library1, domain1Library1Module2);

        JsonObject domain2 = createVertex("DOMAIN2", "domain");
        JsonObject domain2Library1 = createVertex("DOMAIN2-LIB", "library");
        JsonObject domain2Library1Module1 = createVertex("DOMAIN2-LIB-M1", "library");
        JsonObject domain2Library1Module2 = createVertex("DOMAIN2-LIB-M2", "library");

        JsonObject domain2Backend1 = createVertex("DOMAIN2-BACKEND1", "backend");
        JsonObject domain2Backend1Module1 = createVertex("DOMAIN2-BACKEND1-M1", "library");
        JsonObject domain2Backend1Module2 = createVertex("DOMAIN2-BACKEND1-M2", "library");

        JsonObject domain2Backend2 = createVertex("DOMAIN2-BACKEND2", "backend");
        JsonObject domain2Backend2Module1 = createVertex("DOMAIN2-BACKEND2-M1", "library");

        JsonObject lib1 = createVertex("LIB1", "library");
        JsonObject lib1_1 = createVertex("LIB1_1", "library");
        JsonObject lib2 = createVertex("LIB2", "library");
        JsonObject lib2_1 = createVertex("LIB2_1", "library");

        this.graph.addVertexUnderRoot(domain2);
        this.graph.addVertex(domain2, domain2Library1);
        this.graph.addVertex(domain2, domain2Backend1);
        this.graph.addVertex(domain2, domain2Backend2);

        createRelation("e102", "module", domain2Library1, domain2Library1Module1);
        createRelation("e103", "module", domain2Library1, domain2Library1Module2);
        createRelation("e104", "module", domain2Backend1, domain2Backend1Module1);
        createRelation("e105", "module", domain2Backend1, domain2Backend1Module2);
        createRelation("e106", "module", domain2Backend2, domain2Backend2Module1);


        createRelation("e200", "dependence", domain1Library1Module1, lib1);
        createRelation("e201", "dependence", domain2Backend1Module1, lib2);
        createRelation("e203", "dependence", lib1, lib1_1);
        createRelation("e204", "dependence", lib1, domain2Library1Module1);
        createRelation("e205", "dependence", lib2, lib1);
        createRelation("e206", "dependence", lib2, lib2_1);
        createRelation("e207", "dependence", domain2Library1Module2, lib1_1);
        createRelation("e208", "dependence", lib1_1, lib2);

        createRelation("e300", "dependence", domain2Backend1Module1, domain2Library1Module2);
        createRelation("e301", "dependence", domain2Backend1Module2, domain2Backend1Module1);
        createRelation("e302", "dependence", domain2Backend2Module1, domain2Backend1Module2);
    }

    // id, type, artifactId, groupId, package, version
    protected JsonObject createVertex(String id, String type) {
        JsonObject result = new JsonObject()
            .put("id", id)
            .put("type", type);
        return result;
    }

    protected JsonObject createRelation(String id, String type, JsonObject source, JsonObject target) {
        JsonObject edge = createVertex(id, type);
        this.graph.addEdge(source, target, edge);
        return edge;
    }

    protected void print(List<?> list) {
        System.out.println("List size: " + list.size());
        for (Object obj : list) {
            System.out.println(obj);
        }
    }

    protected void assertPath(Path path, Object ... values) {
        for (int i=0 ; i<values.length ; i++) {
            Assertions.assertEquals(path.get(i), values[i]);
        }
    }
}
