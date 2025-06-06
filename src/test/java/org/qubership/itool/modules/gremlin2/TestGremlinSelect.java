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

import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.*;

import org.qubership.itool.modules.gremlin2.graph.GraphTraversal;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestGremlinSelect extends AbstractGremlinTest {

    @Test
    public void testSelect_out() {
        /*
        v1_marko -knows-> v4_josh -created->    v3_lop (lang)
                                  -created->    v5_ripple (lang)
                                  -maintained-> v6_linux (os)
         */
        GraphTraversal<JsonObject, JsonObject> traversal = V().as("a").out().as("b").out().as("c").hasKey("lang");
        Assertions.assertEquals(2, traversal.toList().size());

        GraphTraversal<JsonObject, JsonObject> traversal_a = traversal.select("a");
        Assertions.assertEquals("v1", traversal_a.toList().get(0).getString("id"));
        Assertions.assertEquals("v1", traversal_a.toList().get(1).getString("id"));

        GraphTraversal<JsonObject, JsonObject> traversal_b = traversal.select("b");
        Assertions.assertEquals("v4", traversal_b.toList().get(0).getString("id"));
        Assertions.assertEquals("v4", traversal_b.toList().get(1).getString("id"));

        GraphTraversal<JsonObject, JsonObject> traversal_c = traversal.select("c");
        Assertions.assertEquals("v3", traversal_c.toList().get(0).getString("id"));
        Assertions.assertEquals("v5", traversal_c.toList().get(1).getString("id"));
    }


    @Test
    public void testSelect_in() {
        /*
        v1_marko -knows-> v4_josh -created->    v3_lop (lang)
                                  -created->    v5_ripple (lang)
                                  -maintained-> v6_linux (os)
         */
        GraphTraversal<JsonObject, JsonObject> traversal = V().as("a").in("maintained").as("b").in().as("c");
        Assertions.assertEquals(1, traversal.toList().size());

        GraphTraversal<JsonObject, JsonObject> traversal_a = traversal.select("a");
        Assertions.assertEquals("v6", traversal_a.toList().get(0).getString("id"));

        GraphTraversal<JsonObject, JsonObject> traversal_b = traversal.select("b");
        Assertions.assertEquals("v4", traversal_b.toList().get(0).getString("id"));

        GraphTraversal<JsonObject, JsonObject> traversal_c = traversal.select("c");
        Assertions.assertEquals("v1", traversal_c.toList().get(0).getString("id"));
    }

    @Test
    public void testSelect_outE() {
        GraphTraversal<JsonObject, JsonObject> traversal = V().as("a")
            .outE("knows").as("b")
            .inV().as("c").out().as("d").hasKey("lang");
        Assertions.assertEquals(2, traversal.toList().size());

        GraphTraversal<JsonObject, JsonObject> traversal_a = traversal.select("a");
        Assertions.assertEquals("v1", traversal_a.toList().get(0).getString("id"));
        Assertions.assertEquals("v1", traversal_a.toList().get(1).getString("id"));

        GraphTraversal<JsonObject, JsonObject> traversal_b = traversal.select("b");
        Assertions.assertEquals("e1", traversal_b.toList().get(0).getString("id"));
        Assertions.assertEquals("e1", traversal_b.toList().get(1).getString("id"));

        GraphTraversal<JsonObject, JsonObject> traversal_c = traversal.select("c");
        Assertions.assertEquals("v4", traversal_c.toList().get(0).getString("id"));
        Assertions.assertEquals("v4", traversal_c.toList().get(1).getString("id"));

        GraphTraversal<JsonObject, JsonObject> traversal_d = traversal.select("d");
        Assertions.assertEquals("v3", traversal_d.toList().get(0).getString("id"));
        Assertions.assertEquals("v5", traversal_d.toList().get(1).getString("id"));

    }

    @Test
    public void testSelect_inE() {
        GraphTraversal<JsonObject, JsonObject> traversal = V().as("a").inE("maintained").as("b")
            .outV().as("c").in().as("d");
        Assertions.assertEquals(1, traversal.toList().size());

        GraphTraversal<JsonObject, JsonObject> traversal_a = traversal.select("a");
        Assertions.assertEquals("v6", traversal_a.toList().get(0).getString("id"));

        GraphTraversal<JsonObject, JsonObject> traversal_b = traversal.select("b");
        Assertions.assertEquals("e4", traversal_b.toList().get(0).getString("id"));

        GraphTraversal<JsonObject, JsonObject> traversal_c = traversal.select("c");
        Assertions.assertEquals("v4", traversal_c.toList().get(0).getString("id"));

        GraphTraversal<JsonObject, JsonObject> traversal_d = traversal.select("d");
        Assertions.assertEquals("v1", traversal_d.toList().get(0).getString("id"));
    }

    @Test
    public void testSelectEmpty() {
        List<Map<String, Object>> traversal = V().as("a").inE("maintained").as("b")
            .outV().as("c").in().as("d").out("ssssss")
            .select("a", "b", "c", "d").toList();
        assertEquals(0, traversal.size());
    }

    @Test
    public void testSelectVertexThenOut() {
        // A is v1_marko, B is v4_josh, C is v6_linux
        // SelectScalarStep -> VertexStep
        List<Map<String, Object>> result =
                 V().as("B").id().as("BI").in("knows").as("A").id().as("AI")
                .select("B").out("maintained").as("C").id().as("CI")
                .select("AI", "BI", "CI").toList();
        List<Map<String, Object>> expected = List.of(
                Map.of("AI", "v1", "BI", "v4", "CI", "v6"));
        assertEquals(expected, result);

        // SelectStep -> VertexStep
        result = V().as("B").id().as("BI").in("knows").as("A").id().as("AI")
                .select("B", "A").value("B").out("maintained").as("C").id().as("CI")
                .select("AI", "BI", "CI").toList();
        assertEquals(expected, result);
    }

    @Test
    public void testSelectVertexThenOutE() {
        // A is v1_marko, B is v4_josh, C is e4_maintained, D is v6_linux
        // SelectScalarStep -> EdgeStep
        List<Map<String, Object>> result =
                 V().as("B").id().as("BI").in("knows").as("A").id().as("AI")
                .select("B").outE("maintained").as("C").id().as("CI")
                .inV().as("D").id().as("DI")
                .select("AI", "BI", "CI", "DI").toList();
        List<Map<String, Object>> expected = List.of(
                Map.of("AI", "v1", "BI", "v4", "CI", "e4", "DI", "v6"));
        assertEquals(expected, result);

        // SelectStep -> EdgeStep
        result = V().as("B").id().as("BI").in("knows").as("A").id().as("AI")
                .select("B", "A").value("B").outE("maintained").as("C").id().as("CI")
                .inV().as("D").id().as("DI")
                .select("AI", "BI", "CI", "DI").toList();
        assertEquals(expected, result);
    }

    @Test
    public void testSelectEdgeThenOutV() {
        // A is v1_marko, B is e1_knows, C is v4_josh
        // SelectScalarStep -> EdgeVertexStep
        List<Map<String, Object>> result =
                 E().as("B").hasType("knows").id().as("BI").outV().as("A").id().as("AI")
                .select("B").inV().as("C").id().as("CI")
                .select("AI", "BI", "CI").toList();
        List<Map<String, Object>> expected = List.of(
                Map.of("AI", "v1", "BI", "e1", "CI", "v4"));
        assertEquals(expected, result);

        // SelectStep -> EdgeVertexStep
        result = E().as("B").hasType("knows").id().as("BI").outV().as("A").id().as("AI")
                .select("B", "A").value("B").inV().as("C").id().as("CI")
                .select("AI", "BI", "CI").toList();
        assertEquals(expected, result);
    }

}
