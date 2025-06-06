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

public class TestGremlinVertexInOut extends AbstractGremlinTest {

    @Test
    void testOutV_Vertex() {
        Assertions.assertThrows(GremlinException.class, () -> {
            V("v1").outV().toList();
        });
    }

    @Test
    void testOut() {
        GraphTraversal<JsonObject, JsonObject> traversal = V("v1").out();
        Assertions.assertEquals(1 , traversal.toList().size());
        traversal = V("v4").out();
        Assertions.assertEquals(3 , traversal.toList().size());
    }

    @Test
    void testOutType() {
        GraphTraversal<JsonObject, JsonObject> traversal = V("v1").out("knows");
        Assertions.assertEquals(1 , traversal.toList().size());
        traversal = V("v4").out("created");
        Assertions.assertEquals(2 , traversal.toList().size());
    }

    @Test
    void testInV_Vertex() {
        Assertions.assertThrows(GremlinException.class, () -> {
            V("v4").inV().toList();
        });
    }

    @Test
    void testIn() {
        GraphTraversal<JsonObject, JsonObject> traversal = V("v4").in();
        Assertions.assertEquals(1 , traversal.toList().size());
        Assertions.assertEquals("v1", traversal.next().getString("id"));
        traversal = V("v5", "v3").in();
        Assertions.assertEquals(2 , traversal.toList().size());
        Assertions.assertEquals("v4", traversal.toList().get(0).getString("id"));
        Assertions.assertEquals("v4", traversal.toList().get(1).getString("id"));
    }

    @Test
    void testInType() {
        GraphTraversal<JsonObject, JsonObject> traversal = V("v4").in("knows");
        Assertions.assertEquals(1 , traversal.toList().size());
        Assertions.assertEquals("v1", traversal.next().getString("id"));
        traversal = V("v5", "v3").in("created");
        Assertions.assertEquals(2 , traversal.toList().size());
        Assertions.assertEquals("v4", traversal.toList().get(0).getString("id"));
        Assertions.assertEquals("v4", traversal.toList().get(1).getString("id"));
    }

    @Test
    void testBoth() {
        List<String> result = V("v4").both().id().order().toList();
        Assertions.assertEquals(4 , result.size());
        Assertions.assertEquals("v1", result.get(0));
        Assertions.assertEquals("v3", result.get(1));
        Assertions.assertEquals("v5", result.get(2));
        Assertions.assertEquals("v6", result.get(3));
    }

    @Test
    void testBothType() {
        List<String> result = V("v4").both("knows").id().toList();
        Assertions.assertEquals(1 , result.size());
        Assertions.assertEquals("v1", result.get(0));
    }

}
