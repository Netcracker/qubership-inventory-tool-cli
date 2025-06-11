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

public class TestGremlinEdge extends AbstractGremlinTest {

    @Test
    void testE() {
        List<JsonObject> result = E().toList();
        for (JsonObject edge : result) {
            Assertions.assertTrue(edge.getString("id").startsWith("e"));
        }
    }

    @Test
    void testOutE() {
        GraphTraversal<JsonObject, JsonObject> traversal = V("v4").outE();
        Assertions.assertEquals(3 , traversal.toList().size());
    }

    @Test
    void testOutEType() {
        GraphTraversal<JsonObject, JsonObject> traversal = V("v4").outE("maintained");
        Assertions.assertEquals(1 , traversal.toList().size());
        Assertions.assertTrue(traversal.next().containsKey("yearFrom"));
        Assertions.assertTrue(traversal.has("yearFrom", "2000").next().containsKey("yearFrom"));
        traversal = V("v4").outE("created");
        Assertions.assertEquals(2 , traversal.toList().size());
    }

    @Test
    void testOutEInV_1() {
        GraphTraversal<JsonObject, JsonObject> traversal = V("v4").outE("maintained").inV();
        Assertions.assertEquals(1 , traversal.toList().size());
        Assertions.assertEquals("v6", traversal.next().getString("id"));
    }

    @Test
    void testOutEInV_2() {
        GraphTraversal<JsonObject, JsonObject> traversal = V("v4").outE().inV();
        List<JsonObject> list = traversal.toList();
        Assertions.assertEquals(3, list.size());
        Assertions.assertEquals("v3", list.get(0).getString("id"));
        Assertions.assertEquals("v5", list.get(1).getString("id"));
        Assertions.assertEquals("v6", list.get(2).getString("id"));

        traversal = V("v4").out();
        list = traversal.toList();
        Assertions.assertEquals(3 , list.size());
        Assertions.assertEquals("v3", list.get(0).getString("id"));
        Assertions.assertEquals("v5", list.get(1).getString("id"));
        Assertions.assertEquals("v6", list.get(2).getString("id"));
    }

    @Test
    void testOutEOutV() {
        GraphTraversal<JsonObject, JsonObject> traversal = V("v4").outE("maintained").outV();
        Assertions.assertEquals(1 , traversal.toList().size());
        Assertions.assertEquals("v4", traversal.next().getString("id"));
    }

    @Test
    void testInEOutV() {
        GraphTraversal<JsonObject, JsonObject> traversal = V("v4").inE().outV();
        Assertions.assertEquals(1 , traversal.toList().size());
        Assertions.assertEquals("v1", traversal.next().getString("id"));
    }

    @Test
    void testInEInV() {
        GraphTraversal<JsonObject, JsonObject> traversal = V("v4").inE().inV();
        Assertions.assertEquals(1 , traversal.toList().size());
        Assertions.assertEquals("v4", traversal.next().getString("id"));
    }

    @Test
    void testBothE() {
        GraphTraversal<JsonObject, JsonObject> traversal = V("v4").bothE();
        Assertions.assertEquals(4 , traversal.toList().size());

        traversal = V("v4").bothE("created");
        Assertions.assertEquals(2 , traversal.toList().size());

        traversal = V("v4").bothE("created", "knows");
        Assertions.assertEquals(3 , traversal.toList().size());
    }

    @Test
    void testOut() {
        Assertions.assertThrows(GremlinException.class, () -> {
            V("v1").outV().out().toList();
        });
    }

    @Test
    void testIn() {
        Assertions.assertThrows(GremlinException.class, () -> {
            V("v1").outV().in().toList();
        });
    }

    @Test
    void testOutVOutE() {
        Assertions.assertThrows(GremlinException.class, () -> {
            V("v1").outV().outE().toList();
        });
    }

    @Test
    void testOutVInE() {
        Assertions.assertThrows(GremlinException.class, () -> {
            V("v1").outV().inE().toList();
        });
        Assertions.assertThrows(GremlinException.class, () -> {
            V("v1").outV().inE("dd").toList();
        });
    }

    @Test
    void testHasType() {
        JsonObject edgeJson = V("v4").inE().as("edge").hasType("knows").outV().next();
        Assertions.assertNotNull(edgeJson);
    }

    @Test
    void testHasIdMultiple() {
        JsonObject edgeJson = V("v4").inE().as("edge").hasId("e1").outV().next();
        Assertions.assertNotNull(edgeJson);
    }

    @Test
    void testHasNotIdMultiple() {
        JsonObject edgeJson = V("v4").inE().as("edge").hasNotId("e1").outV().next();
        Assertions.assertNull(edgeJson);
    }

    @Test
    void testHasKeys() {
        JsonObject edgeJson = V("v4").inE().as("edge").hasKey("type").outV().next();
        Assertions.assertNotNull(edgeJson);
    }

    @Test
    void testHasKeysMultiple() {
        JsonObject edgeJson = V("v4").inE().as("edge").hasKey("type", "relation").outV().next();
        Assertions.assertNotNull(edgeJson);
    }

    @Test
    void testHasKeysNot() {
        JsonObject edgeJson = V("v4").inE().as("edge").hasNot("type").outV().next();
        Assertions.assertNull(edgeJson);
    }

    @Test
    void testHasKeyValue() {
        JsonObject edgeJson = V("v4").inE().as("edge").has("type", "knows").outV().next();
        Assertions.assertNotNull(edgeJson);
    }

    @Test
    void testHasTypeKeyValue() {
        JsonObject edgeJson = V("v4").inE().as("edge").has("knows", "relation", "parent").outV().next();
        Assertions.assertNotNull(edgeJson);
    }

    @Test
    void testHasKeyPredicate() {
        JsonObject edgeJson = V("v4").inE().as("edge").has("relation", P.containing("are")).outV().next();
        Assertions.assertNotNull(edgeJson);
    }

}
