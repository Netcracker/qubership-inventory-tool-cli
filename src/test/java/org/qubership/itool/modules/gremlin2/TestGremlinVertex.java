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
import org.qubership.itool.modules.gremlin2.graph.__;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class TestGremlinVertex extends AbstractGremlinTest {

    @Test
    void testId() {
        String id = V("v1").id().next();
        Assertions.assertEquals("v1", id);
    }

    @Test
    void testType() {
        String type = V("v1").type().next();
        Assertions.assertEquals("person", type);
    }

    @Test
    void testToList() {
        Assertions.assertEquals(6, V().toList().size());
    }

    @Test
    void testV() {
        GraphTraversal<JsonObject, JsonObject> traversal = V();
        Assertions.assertEquals(6 , traversal.toList().size());
    }

    @Test
    void testVId() {
        GraphTraversal<JsonObject, JsonObject> traversal = V("v1");
        Assertions.assertEquals(1 , traversal.toList().size());
        traversal = V("v1500000");
        Assertions.assertEquals(0 , traversal.toList().size());
    }

    @Test
    void testNext() {
        Assertions.assertEquals("root", V().next().getString("id"));
    }

    @Test
    void testHasId() {
        GraphTraversal<JsonObject, JsonObject> traversal = V().hasId("v4");
        Assertions.assertEquals(1 , traversal.toList().size());
        Assertions.assertEquals("v4", traversal.next().getString("id"));
    }

    @Test
    void testHasIdMultiple() {
        GraphTraversal<JsonObject, JsonObject> traversal = V().hasId("v4", "v5");
        Assertions.assertEquals(2 , traversal.toList().size());
        Assertions.assertEquals("v4", traversal.toList().get(0).getString("id"));
        Assertions.assertEquals("v5", traversal.toList().get(1).getString("id"));
    }

    @Test
    void testHasNotId() {
        GraphTraversal<JsonObject, JsonObject> traversal = V().hasNotId("v4");
        boolean founded = false;
        for (JsonObject jsonObject : traversal.toList()) {
            if ("v4".equals(jsonObject.getString("id"))) {
                founded = true;
                break;
            }
        }
        Assertions.assertEquals(5, traversal.toList().size());
        Assertions.assertFalse(founded);
    }

    @Test
    void testHasKeys() {
        GraphTraversal<JsonObject, JsonObject> traversal = V().hasKey("age");
        Assertions.assertEquals(2, traversal.toList().size());
        Assertions.assertEquals("v1", traversal.toList().get(0).getString("id"));
        Assertions.assertEquals("v4", traversal.toList().get(1).getString("id"));
    }

    @Test
    void testHasKeysMultiple() {
        GraphTraversal<JsonObject, JsonObject> traversal = V().hasKey("age", "lang");
        Assertions.assertEquals(0, traversal.toList().size());
        traversal = V().hasKey("lang");
        Assertions.assertEquals(2, traversal.toList().size());
        traversal = V().hasKey("age");
        Assertions.assertEquals(2, traversal.toList().size());
    }

    @Test
    void testHasKeysNot() {
        GraphTraversal<JsonObject, JsonObject> traversal = V().hasNot("age");
        boolean founded = false;
        for (JsonObject jsonObject : traversal.toList()) {
            if (jsonObject.containsKey("age")) {
                founded = true;
                break;
            }
        }
        Assertions.assertEquals(4, traversal.toList().size());
        Assertions.assertFalse(founded);
    }

    @Test
    void testHas() {
        GraphTraversal<JsonObject, JsonObject> traversal = V().has("name", "marko");
        Assertions.assertEquals(1 , traversal.toList().size());
        Assertions.assertEquals("v1", traversal.next().getString("id"));
    }

    @Test
    void testHasTypeValue() {
        GraphTraversal<JsonObject, JsonObject> traversal = V().has("person", "name", "marko");
        Assertions.assertEquals(1 , traversal.clone().toList().size());
        Assertions.assertEquals("v1", traversal.clone().next().getString("id"));

        traversal = V().has("person", "name", "ripple");
        Assertions.assertEquals(0 , traversal.toList().size());
    }

    @Test
    void testHasType() {
        GraphTraversal<JsonObject, JsonObject> traversal = V().hasType("person");
        Assertions.assertEquals(2 , traversal.toList().size());
    }

    @Test
    void testHasTypeMultiple() {
        GraphTraversal<JsonObject, JsonObject> traversal = V().hasType("person", "soft");
        Assertions.assertEquals(4 , traversal.toList().size());
    }

    @Test
    void testNot() {
        List<JsonObject> traversal = V().not(__.hasType("person", "soft")).toList();
        Assertions.assertEquals(2 , traversal.size());
        Assertions.assertNotEquals("person" , traversal.get(0));
        Assertions.assertNotEquals("person" , traversal.get(1));
        Assertions.assertNotEquals("soft" , traversal.get(0));
        Assertions.assertNotEquals("soft" , traversal.get(1));
    }

    @Test
    void testOrder() {
        List<JsonObject> res1 = V().hasType("person").toList();
        Assertions.assertEquals(2, res1.size());
        // "marko" and "josh", in unspecified order
        Assertions.assertEquals(
                Set.of("marko", "josh"),
                Set.of(res1.get(0).getString("name"), res1.get(1).getString("name")));

        List<String> res2 = V().hasType("person").order().<String>value("name").toList();
        Assertions.assertEquals(2, res2.size());
        Assertions.assertEquals("marko", res2.get(0));
        Assertions.assertEquals("josh", res2.get(1));
    }

    @Test
    void testCount() {
        Integer vertexCount = this.graph.getVertexCount();
        Long traversalCount = V().count().next();
        Assertions.assertEquals(vertexCount.longValue(), traversalCount.longValue());
    }

    @Test
    void testOr() {
        @SuppressWarnings("unchecked")
        List<JsonObject> result = V().out()
            .<JsonObject>or(
                __.has("name", "lop")
                , __.has("name", "josh")
            )
            .toList();
//        print(result);
        Assertions.assertEquals(2, result.size());
        Assertions.assertEquals("v4", result.get(0).getString("id"));
        Assertions.assertEquals("v3", result.get(1).getString("id"));
    }

    @Test
    void testTail() {
        GraphTraversal<JsonObject, JsonObject> traversal = V().hasType("person", "soft");
        Assertions.assertEquals(4, traversal.clone().count().next());

        List<String> result = traversal.clone().<String>value("id").toList();
        Assertions.assertEquals("v1", result.get(0));
        Assertions.assertEquals("v4", result.get(1));
        Assertions.assertEquals("v3", result.get(2));
        Assertions.assertEquals("v5", result.get(3));

        result = traversal.tail(2).<String>value("id").toList();
        Assertions.assertEquals(2, result.size());
        Assertions.assertEquals("v3", result.get(0));
        Assertions.assertEquals("v5", result.get(1));
    }

    @Test
    void testLimit() {
        GraphTraversal<JsonObject, JsonObject> traversal = V().hasType("person", "soft");
        Assertions.assertEquals(4, traversal.clone().count().next());

        List<String> result = traversal.clone().<String>value("id").toList();
        Assertions.assertEquals("v1", result.get(0));
        Assertions.assertEquals("v4", result.get(1));
        Assertions.assertEquals("v3", result.get(2));
        Assertions.assertEquals("v5", result.get(3));

        result = traversal.limit(2).<String>value("id").toList();
        Assertions.assertEquals(2, result.size());
        Assertions.assertEquals("v1", result.get(0));
        Assertions.assertEquals("v4", result.get(1));
    }

    @Test
    void testRange() {
        GraphTraversal<JsonObject, JsonObject> traversal = V().hasType("person", "soft");
        Assertions.assertEquals(4, traversal.clone().count().next());

        List<String> result = traversal.clone().<String>value("id").toList();
        Assertions.assertEquals("v1", result.get(0));
        Assertions.assertEquals("v4", result.get(1));
        Assertions.assertEquals("v3", result.get(2));
        Assertions.assertEquals("v5", result.get(3));

        result = traversal.range(1, 3).<String>value("id").toList();
        print(result);
        Assertions.assertEquals(2, result.size());
        Assertions.assertEquals("v4", result.get(0));
        Assertions.assertEquals("v3", result.get(1));
    }

    @Test
    void testGroupCount() {
        List<Map<Object, Object>> result = V().inE().outV()
            .group().by("id").by(__.count())
            .toList();
        print(result);
        // {root=1, v1=1, v4=3}
        Assertions.assertEquals(3, result.get(0).size());
        Assertions.assertEquals(1l, result.get(0).get("root"));
        Assertions.assertEquals(1l, result.get(0).get("v1"));
        Assertions.assertEquals(3l, result.get(0).get("v4"));
    }

    @SuppressWarnings("rawtypes")
    @Test
    void testGroupByJsonPointer() {
        List<Map<Object, Object>> result = V().group().by("/details/document").toList();
//        print(result);
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("123456789", result.get(0).keySet().iterator().next());
        List list = (List)result.get(0).get("123456789");
        Assertions.assertEquals("v1", ((JsonObject)list.get(0)).getString("id"));
    }

    @Test
    void testGroupByJsonPointerCount() {
        List<Map<Object, Object>> result =
            V().group().by("/details/document").by(__.count()).toList();
//        print(result);
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("123456789", result.get(0).keySet().iterator().next());
        Long count = (Long)result.get(0).get("123456789");
        Assertions.assertEquals(1l, count);
    }

}
