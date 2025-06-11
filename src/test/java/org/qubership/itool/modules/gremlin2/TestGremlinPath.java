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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.qubership.itool.modules.gremlin2.graph.__.outE;

public class TestGremlinPath extends AbstractGremlinTest {

    @Test
    void test_path() {
        List<Path> result = V("root").as("L1")
            .out().as("L2")
            .out()
            .path().toList();
//        print(result);
//        Path{
//            1) [L1] : {"id":"root","type":"root","name":"root"}
//            2) [L2] : {"id":"v1","name":"marko","age":29,"type":"person","details":{"document":"123456789","weight":78}}
//            3) [] : {"id":"v4","name":"josh","age":32,"type":"person"}
//        }
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("root", ((JsonObject)result.get(0).get(0)).getString("id"));
        Assertions.assertEquals("v1", ((JsonObject)result.get(0).get(1)).getString("id"));
        Assertions.assertEquals("v4", ((JsonObject)result.get(0).get(2)).getString("id"));
        Assertions.assertEquals("root", ((JsonObject)result.get(0).get("L1")).getString("id"));
        Assertions.assertEquals("v1", ((JsonObject)result.get(0).get("L2")).getString("id"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void test_coalesce() {
        List<Path> result = V("v1")
            .coalesce(
                outE("created")
                , outE("knows"))
            .inV()
            .path()
            .toList();
//        print(result);
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("v1", ((JsonObject)result.get(0).get(0)).getString("id"));
        Assertions.assertEquals("e1", ((JsonObject)result.get(0).get(1)).getString("id"));
        Assertions.assertEquals("v4", ((JsonObject)result.get(0).get(2)).getString("id"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void test_coalesce_value() {
        List<JsonObject> result = V("v1")
            .coalesce(
                outE("created").value("id")
                , outE("knows").value("id"))
            .inV()
            .toList();
//        print(result);
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("v4", result.get(0).getString("id"));

    }
}
