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

import org.junit.jupiter.api.*;

import org.qubership.itool.modules.gremlin2.graph.__;

import java.util.List;
import java.util.Map;

import static org.qubership.itool.modules.gremlin2.P.eq;

public class TestGremlinVertexAdvanced extends AbstractGremlinTest {

    @BeforeEach
    public void cleanup() {
        this.graph.clear();
        createOneVertexTwoPathGraph();
    }

    @Test
    void testHasKeysJsonPointer() {
        List result = V().hasType("database").as("db")
            .in().hasKey("/details").as("c").select("db", "c")
            .values("db:/db/id", "component:/c/id").toList();
        Assertions.assertEquals(2, result.size());
    }

    @Test
    void testHasKeys() {
        List<Map<Object, Object>> result = V().hasType("database").as("db")
            .in().hasKey("details").as("c")
            .select("db", "c").values("db:/db/id", "component:/c/id")
            .toList();
        Assertions.assertEquals(2, result.size());
    }

    @Test
    void testHasType() {
        List<Map<Object, Object>> result = V().hasType("database").as("db")
            .in().hasType("backend").as("c")
            .select("db", "c").values("db:/db/id", "component:/c/id").toList();
        Assertions.assertEquals(2, result.size());
    }

    @Test
    void testHasTypeValue() {
        List<Map<Object, Object>> result = V().hasType("database").as("db")
            .in().has("backend", "type", "backend").as("c")
            .select("db", "c").values("db:/db/id", "component:/c/id").toList();
        Assertions.assertEquals(2, result.size());
    }

    @Test
    void testHasTypeValueJsonPointer() {
        List<Map<Object, Object>> result = V().hasType("database").as("db")
            .in().has("backend", "/type", "backend").as("c")
            .select("db", "c").values("db:/db/id", "component:/c/id").toList();
        Assertions.assertEquals(2, result.size());
    }

    @Test
    void testHasKeyValue() {
        List<Map<Object, Object>> result = V().hasType("database").as("db")
            .in().has("type", "backend").as("c")
            .select("db", "c").values("db:/db/id", "component:/c/id").toList();
        Assertions.assertEquals(2, result.size());
    }

    @Test
    void testHasKeyValueJsonPointer() {
        List<Map<Object, Object>> result = V().hasType("database").as("db")
            .in().has("/type", "backend").as("c")
            .select("db", "c").values("db:/db/id", "component:/c/id").toList();
        Assertions.assertEquals(2, result.size());
    }

    @Test
    void testHasPredicate() {
        List<Map<Object, Object>> result = V().hasType("database").as("db")
            .in().has("type", eq("backend")).as("c")
            .select("db", "c").values("db:/db/id", "component:/c/id").toList();
        Assertions.assertEquals(2, result.size());
    }

    @Test
    void testHasPredicateJsonPointer() {
        List<Map<Object, Object>> result = V().hasType("database").as("db")
            .in().has("/type", eq("backend")).as("c")
            .select("db", "c").values("db:/db/id", "component:/c/id").toList();
        Assertions.assertEquals(2, result.size());
    }

    @Test
    void testHasId() {
        List<Map<Object, Object>> result = V().hasType("database").as("db")
            .in().hasId("c1").as("c").select("db", "c")
            .values("db:/db/id", "component:/c/id").toList();
        Assertions.assertEquals(2, result.size());
    }

    @Test
    void testHasNot() {
        List<Map<Object, Object>> result = V().hasType("database").as("db")
            .in().hasNot("empty").as("c").select("db", "c")
            .values("db:/db/id", "component:/c/id").toList();
        Assertions.assertEquals(4, result.size());
    }

    @Test
    void testHasNotJsonPointer() {
        List<Map<Object, Object>> result = V().hasType("database").as("db")
            .in().hasNot("/empty").as("c").select("db", "c")
            .values("db:/db/id", "component:/c/id").toList();
        Assertions.assertEquals(4, result.size());
    }

    @Test
    void testNotHasNot() {
        List<Map<Object, Object>> result = V().hasType("database").as("db")
            .in().not(__.hasNot("empty")).as("c")
            .select("db", "c").values("db:/db/id", "component:/c/id").toList();
        Assertions.assertEquals(0, result.size());
    }

}
