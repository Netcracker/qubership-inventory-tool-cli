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

import static org.qubership.itool.modules.gremlin2.graph.__.outE;

public class TestsGremlinComplex extends AbstractGremlinTest {

    @Test
    void testHasOutHasType() {
        GraphTraversal<JsonObject, JsonObject> traversal =
            V().has("name", "marko").out().out("maintained")
            .has("name", "linux");
        Assertions.assertEquals(1 , traversal.toList().size());
        Assertions.assertEquals("v6" , traversal.next().getString("id"));
    }

    @Test
    void testUnion() {
        this.graph.clear();
        createComplexGraph();

        List<String> result = V("v4")
            .union(
                __.inE("knows").outV() // marko
                , __.out("created") // ripple, lop
            )
            .<String>value("name")
            .toList();

        this.graph.traversal().V().union(outE());

        Assertions.assertEquals(3, result.size());
        Assertions.assertEquals("marko" , result.get(0));
        Assertions.assertEquals("ripple" , result.get(1));
        Assertions.assertEquals("lop" , result.get(2));
    }

    @Test
    void testUnionValue() {
        this.graph.clear();
        createComplexGraph();

        List<String> result = V("v4")
            .<String>union(
                __.inE("knows").outV().value("name"), // marko
                __.out("created").value("name") // ripple, lop
            )
            .toList();

        Assertions.assertEquals(3, result.size());
        Assertions.assertEquals("marko" , result.get(0));
        Assertions.assertEquals("ripple" , result.get(1));
        Assertions.assertEquals("lop" , result.get(2));
    }

    @Test
    void testUnionValueJsonObject() {
        this.graph.clear();
        createComplexGraph();

        List<Map<Object, Object>> result = V("v4")
            .union(
                __.inE("knows").outV().values("name", "type"), // marko:person
                __.out("created").values("name", "type") // ripple:software, lop:software
            )
            .toList();

        Assertions.assertEquals(3, result.size());
        Assertions.assertEquals("marko" , result.get(0).get("name"));
        Assertions.assertEquals("ripple" , result.get(1).get("name"));
        Assertions.assertEquals("lop" , result.get(2).get("name"));
        Assertions.assertEquals("person" , result.get(0).get("type"));
        Assertions.assertEquals("software" , result.get(1).get("type"));
        Assertions.assertEquals("software" , result.get(2).get("type"));
    }

}
