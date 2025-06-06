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
import org.junit.jupiter.api.*;

import org.qubership.itool.modules.gremlin2.graph.GraphTraversal;

import java.util.Collections;
import java.util.List;

import static org.qubership.itool.modules.gremlin2.P.*;
import static org.qubership.itool.modules.gremlin2.graph.__.out;
import static org.qubership.itool.modules.gremlin2.graph.__.select;

public class TestGremlinPredicates extends AbstractGremlinTest {

    @Test
    void testWithin() {
        GraphTraversal<JsonObject, JsonObject> traversal = V().has("name", within("josh", "ripple"));
        Assertions.assertEquals(2 , traversal.toList().size());
        Assertions.assertEquals("josh", traversal.toList().get(0).getString("name"));
        Assertions.assertEquals("ripple", traversal.toList().get(1).getString("name"));
    }

    @Test
    void testWithout() {
        GraphTraversal<JsonObject, JsonObject> traversal = V().has("name", without("josh", "ripple"));
        Assertions.assertEquals(4 , traversal.toList().size());
//        System.out.println(traversal.profile());
    }

    @Test
    void testStartingWith() {
        GraphTraversal<JsonObject, JsonObject> traversal = V().has("name", startingWith("jo"));
        Assertions.assertEquals(1 , traversal.toList().size());
        Assertions.assertEquals("josh", traversal.toList().get(0).getString("name"));
    }

    @Test
    void testNotStartingWith() {
        GraphTraversal<JsonObject, JsonObject> traversal = V().has("name", notStartingWith("jo"));
        Assertions.assertEquals(5 , traversal.toList().size());
    }

    @Test
    void testEndingWith() {
        GraphTraversal<JsonObject, JsonObject> traversal = V().has("name", endingWith("osh"));
        Assertions.assertEquals(1 , traversal.toList().size());
        Assertions.assertEquals("josh", traversal.toList().get(0).getString("name"));
//        System.out.println(traversal.profile());
    }

    @Test
    void testNotEndingWith() {
        GraphTraversal<JsonObject, JsonObject> traversal = V().has("name", notEndingWith("osh"));
        Assertions.assertEquals(5 , traversal.toList().size());
    }

    @Test
    void testContaining() {
        // Containing within a string
        GraphTraversal<JsonObject, JsonObject> traversal = V().has("name", containing("os"));
        Assertions.assertEquals(1 , traversal.toList().size());
        Assertions.assertEquals("v4", traversal.next().getString("id"));

        // Containing within an array
        traversal = V().has("array", containing(new JsonObject().put("seq", "3")));
        Assertions.assertEquals(0, traversal.toList().size());
        traversal = V().has("array", containing(new JsonObject().put("seq", "2")));
        Assertions.assertEquals(1, traversal.toList().size());
        traversal = V().has("labels", containing("second"));
        Assertions.assertEquals(2, traversal.toList().size());
        traversal = V().has("labels", containing("first"));
        Assertions.assertEquals(1, traversal.toList().size());
        Assertions.assertEquals("v3", traversal.next().getString("id"));
    }

    @Test
    void testNotContaining() {
        // Not containing within a string
        GraphTraversal<JsonObject, JsonObject> traversal = V().has("name", notContaining("blabla"));
        Assertions.assertEquals(6 , traversal.toList().size());

        // Not containing within an array
        traversal = V().has("array", notContaining(new JsonObject().put("seq", "3")));
        Assertions.assertEquals(1, traversal.toList().size());
        traversal = V().has("array", notContaining(new JsonObject().put("seq", "2")));
        Assertions.assertEquals(0, traversal.toList().size());
        traversal = V().has("labels", notContaining("anything"));
        Assertions.assertEquals(2, traversal.toList().size());
        traversal = V().has("labels", notContaining("second"));
        Assertions.assertEquals(0, traversal.toList().size());
    }

    @Test
    void testsEqString() {
        GraphTraversal<JsonObject, JsonObject> traversal = V().has("id", eq("v1"));
        Assertions.assertEquals(1 , traversal.toList().size());
    }

    @Test
    void testsEqString2() {
        // Correct usage: has(eq(constant))
        List<String> result1 = V("v1").value("type").as("v1type")
                .out().has("type", eq("person")).id().toList();
        Assertions.assertEquals(Collections.singletonList("v4"), result1);

        // Wrong usage: has(eq(stepName))
        List<String> result2 = V("v1").value("type").as("v1type")
                .out().has("type", eq("v1type")).id().toList();
        Assertions.assertEquals(Collections.emptyList(), result2);
    }

    @Test
    void testsEqStepValue() {
        // Usage: where(eq(stepName))
        List<String> result1 = V("v1").value("type").as("v1type")
                .out().as("another").value("type").where(eq("v1type"))
                .select("another").id().toList();
        Assertions.assertEquals(Collections.singletonList("v4"), result1);
    }

    @Test
    void testsEqInteger() {
        GraphTraversal<JsonObject, JsonObject> traversal = V().has("age", eq(32));
        Assertions.assertEquals(1 , traversal.toList().size());
//        System.out.println(traversal.profile());
    }

    @Test
    void testsNotEqString() {
        GraphTraversal<JsonObject, JsonObject> traversal = V().has("id", neq("v1"));
        Assertions.assertEquals(5 , traversal.toList().size());
    }

    @Test
    void testsNotEqInteger() {
        GraphTraversal<JsonObject, JsonObject> traversal = V().has("age", neq(32));
        Assertions.assertEquals(5 , traversal.toList().size());
    }

    @Test
    void testLte() {
        GraphTraversal<JsonObject, JsonObject> traversal = V().has("age", lte(30));
        Assertions.assertEquals(1 , traversal.toList().size());
        Assertions.assertEquals(29 , traversal.toList().get(0).getInteger("age"));
    }

    @Test
    void testGte() {
        GraphTraversal<JsonObject, JsonObject> traversal = V().has("age", gte(30));
        Assertions.assertEquals(1 , traversal.toList().size());
        Assertions.assertEquals(32 , traversal.toList().get(0).getInteger("age"));
    }

    @Test
    void testInside() {
        GraphTraversal<JsonObject, JsonObject> traversal = V().has("age", inside(28, 33));
        Assertions.assertEquals(2 , traversal.toList().size());
        Assertions.assertEquals(29 , traversal.toList().get(0).getInteger("age"));
        Assertions.assertEquals(32 , traversal.toList().get(1).getInteger("age"));
//        System.out.println(traversal.profile());
    }

    @Test
    @Disabled // JsonPointer not supported arrays
    void testArray() {
        this.graph.clear();
        JsonObject element_1 = new JsonObject().put("artifactId", "a1").put("groupId", "g1").put("version", "5.3.5");
        JsonObject element_2 = new JsonObject().put("artifactId", "comonent2-suffix").put("groupId", "g2").put("version", "2021.2.0.1-SNAPSHOT");
        JsonObject dependencies = new JsonObject().put("direct", new JsonArray().add(element_1).add(element_2));

        JsonObject vertex = new JsonObject()
            .put("id", "v1")
            .put("dependencies", dependencies);

        this.graph.addVertexUnderRoot(vertex);
        List<JsonObject> result = V().has("/dependencies/direct[]/artifactId", startingWith("component2")).toList();
        print(result);
    }

    @Test
    void test_eq_traversal() {
        List<JsonObject> result = V("v1").as("A")
            .repeat(
                out().has("type", eq(select("A").type()))
            ).emit()
            .toList();
//        System.out.println(result);
        // [V4]
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("v4", result.get(0).getString("id"));
    }

    @Test
    void test_lteVersion_1() {
        Assertions.assertTrue(
            P.lteVersion("9.4.40.v20210325").test("9.4.39.v20210324")
        );
    }

    @Test
    void test_gteVersion_1() {
        Assertions.assertFalse(
            P.gteVersion("9.4.40.v20210325").test("9.4.39.v20210324")
        );
    }

    @Test
    void test_lteVersion_2() {
        Assertions.assertTrue(
            P.lteVersion("1.0-rc2").test("0.8-rc2")
        );
    }

    @Test
    void test_gteVersion_2() {
        Assertions.assertFalse(
            P.gteVersion("1.0-rc2").test("0.8-rc2")
        );
    }

    @Test
    void test_lteVersion_3() {
        Assertions.assertTrue(
            P.lteVersion("native:1.2.14").test("native:1.2.11")
        );
    }

    @Test
    void test_gteVersion_3() {
        Assertions.assertFalse(
            P.gteVersion("native:1.2.14").test("native:1.2.11")
        );
    }

}
