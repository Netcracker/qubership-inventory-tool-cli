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

import static org.qubership.itool.modules.gremlin2.P.*;
import static org.qubership.itool.modules.gremlin2.graph.__.outE;

public class TestGremlinVertexWhere extends AbstractGremlinTest {

    @BeforeEach
    public void cleanup() {
        this.graph.clear();
        createComplexGraph();
    }

    @Test
    void testPredicateNeq() {
        GraphTraversal<JsonObject, JsonObject> traversal = V("v1").as("a").out("created").in("created");
        Assertions.assertEquals(3, traversal.clone().toList().size());
        Assertions.assertEquals("v1" , traversal.toList().get(0).getString("id"));
        Assertions.assertEquals("v4" , traversal.toList().get(1).getString("id"));
        Assertions.assertEquals("v6" , traversal.toList().get(2).getString("id"));

        traversal = traversal.clone().where(neq("a"));
        Assertions.assertEquals(2, traversal.toList().size());
        Assertions.assertEquals("v4" , traversal.toList().get(0).getString("id"));
        Assertions.assertEquals("v6" , traversal.toList().get(1).getString("id"));
    }

    @Test
    void testPredicateEq() {
        GraphTraversal<JsonObject, JsonObject> traversal = V("v1").as("a").out("created").in("created");
        Assertions.assertEquals(3, traversal.clone().toList().size());
        Assertions.assertEquals("v1" , traversal.toList().get(0).getString("id"));
        Assertions.assertEquals("v4" , traversal.toList().get(1).getString("id"));
        Assertions.assertEquals("v6" , traversal.toList().get(2).getString("id"));

        traversal = traversal.where(eq("a"));
        Assertions.assertEquals(1, traversal.toList().size());
        Assertions.assertEquals("v1" , traversal.toList().get(0).getString("id"));
    }

    @Test
    void testPredicateNeqWrongAs() {
        Assertions.assertThrows(GremlinException.class, () -> {
            V("v1").as("a").out("created").in("created").where(neq("Z")).toList();
        });

    }

    @Test
    void testPredicateWrongAs() {
        Assertions.assertThrows(GremlinException.class, () -> {
            V("v1").as("a").out("created").in("created").where(eq("Z")).toList();
        });

    }

    @Test
    void testFunction() {
        List<JsonObject> result = V().where(outE("created").count().is(eq(2))).toList();
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("josh", result.get(0).getString("name"));
    }

}
