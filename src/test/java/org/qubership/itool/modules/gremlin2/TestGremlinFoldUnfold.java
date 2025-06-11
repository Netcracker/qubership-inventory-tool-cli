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

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import static org.qubership.itool.modules.gremlin2.structure.MapElement.both;
import static org.qubership.itool.modules.gremlin2.structure.MapElement.key;
import static org.qubership.itool.modules.gremlin2.structure.MapElement.value;

public class TestGremlinFoldUnfold extends AbstractGremlinTest {

    @BeforeEach
    @Override
    public void cleanup() {
        super.cleanup();
        JsonObject json = new JsonObject();
        json.put("id", "v10").put("type", "domain")
            .put("components", new JsonArray().add("a").add("b").add("c"));
        this.graph.addVertexUnderRoot(json);
    }

    @Test
    void test_Unfold() {
        List<Object> result = V("v10").value("components")
            .unfold()
            .toList();
//        print(result);
        Assertions.assertEquals(3, result.size());
        Assertions.assertEquals("a", result.get(0));
        Assertions.assertEquals("b", result.get(1));
        Assertions.assertEquals("c", result.get(2));
    }

    @Test
    void test_Fold() {
        List<List<String>> result = V().hasId("v1", "v3").id().fold().toList();
//        print(result);
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(2, result.get(0).size());
        Assertions.assertEquals("v1", result.get(0).get(0));
        Assertions.assertEquals("v3", result.get(0).get(1));
    }

    @Test
    void test_Map_unfold_key() {
        List<Object> result = V().values("components").unfold().by(key).toList();
//        print(result);
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("components", result.get(0));
    }

    @Test
    void test_Map_unfold_value() {
        List<Object> result = V().values("components").unfold().by(value).toList();
//        print(result);
        Assertions.assertEquals(3, result.size());
        Assertions.assertEquals("a", result.get(0));
        Assertions.assertEquals("b", result.get(1));
        Assertions.assertEquals("c", result.get(2));
    }

    @Test
    void test_Map_unfold_both() {
        List<Map<Object, Object>> result = V().group().by("type").by("id").toList();
        Assertions.assertEquals(1, result.size());
        List<Object> unfoldResult = V().group().by("type").by("id").unfold().by(both).toList();
        Assertions.assertEquals(5, unfoldResult.size());
    }

    @Test
    public void testSize() {
        // List
        Integer result = V().out("knows").out("created").fold().size().next();
        Assertions.assertEquals(2, result);

        // JsonArray
        result = V("v10").value("components").size().next();
        Assertions.assertEquals(3, result);

        // Map
        result = V("v10").values("id", "type").size().next();
        Assertions.assertEquals(2, result);

        // JsonObject
        result = V("v10").size().next();
        Assertions.assertEquals(3, result);
    }

}
