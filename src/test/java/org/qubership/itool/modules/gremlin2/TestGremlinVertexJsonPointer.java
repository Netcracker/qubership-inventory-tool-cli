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

public class TestGremlinVertexJsonPointer extends AbstractGremlinTest {

    @Test
    void testHasKeys() {
        GraphTraversal<JsonObject, JsonObject> traversal = V().hasKey("/age");
        Assertions.assertEquals(2, traversal.toList().size());
        Assertions.assertEquals("v1", traversal.toList().get(0).getString("id"));
        Assertions.assertEquals("v4", traversal.toList().get(1).getString("id"));
    }

    @Test
    void testHasKeysMultiple() {
        GraphTraversal<JsonObject, JsonObject> traversal = V().hasKey("/age", "/lang");
        Assertions.assertEquals(0, traversal.toList().size());
    }

    @Test
    void testHasKeysInner() {
        GraphTraversal<JsonObject, JsonObject> traversal = V().hasKey("/details/document");
        Assertions.assertEquals(1, traversal.toList().size());
        Assertions.assertEquals("v1", traversal.toList().get(0).getString("id"));
    }

    @Test
    void testHasKeysNot() {
        GraphTraversal<JsonObject, JsonObject> traversal = V().hasNot("/details/document");
        Assertions.assertEquals(5, traversal.toList().size());
        for (JsonObject json : traversal.toList()) {
            Assertions.assertNotEquals("v1", json.getString("id"));
        }
    }

    @Test
    void testArray() {
        List<Object> result = V("v1").value("/array[]/seq").toList();
        System.out.println(result);

        Assertions.assertEquals(1, result.size());
        Object obj = result.get(0);
        Assertions.assertTrue(obj instanceof List);
        List list = (List)obj;
        Assertions.assertEquals("1", list.get(0));
        Assertions.assertEquals("2", list.get(1));
    }

    @Test
    void testWrongArrayJsonPointer() {
        List<Object> result = V("v1").value("/array/seq").toList();
        System.out.println(result);

        Assertions.assertEquals(0, result.size());
    }

}
