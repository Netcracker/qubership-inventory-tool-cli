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

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.qubership.itool.modules.gremlin2.P.*;
import static org.qubership.itool.modules.gremlin2.graph.__.out;

public class TestComparators extends AbstractGremlinTest {

    @Test
    void testCounts() {
        Set<Map<Object, Object>> gte0 = V().as("v").local(out().count()).is(gte(0)).as("c")
                .select("v", "c").values("name:/v/name", "c").toSet();
        Set<Map<String, Object>> gte0expected = Set.of(
                Map.of("name", "root", "c", 1L),
                Map.of("name", "marko", "c", 1L),
                Map.of("name", "josh", "c", 3L),
                Map.of("name", "lop", "c", 0L),
                Map.of("name", "ripple", "c", 0L),
                Map.of("name", "linux", "c", 0L)
        );

        /*  // Debug the test
        System.out.println(">>>>>> result = "
            + gte0.stream()
            .sorted(Comparator.comparing( (Map<Object, Object> m) -> (String)m.get("name")))
            .map( (Map<Object, Object> m) -> "name=" + m.get("name") + ", c=" + m.get("c") + ":" + m.get("c").getClass().getName())
            .collect(Collectors.toList()));
        System.out.println(">>>>>> expected = "
            + gte0expected.stream()
            .sorted(Comparator.comparing( (Map<String, Object> m) -> (String)m.get("name")))
            .map( (Map<String, Object> m) -> "name=" + m.get("name") + ", c=" + m.get("c") + ":" + m.get("c").getClass().getName())
            .collect(Collectors.toList()));
        */
        assertEquals(gte0expected, gte0);

        Set<Map<Object, Object>> gt0 = V().as("v").local(out().count()).is(gt(0)).as("c")
                .select("v", "c").values("name:/v/name", "c").toSet();
        Set<Map<String, Object>> gt0expected = Set.of(
                Map.of("name", "root", "c", 1L),
                Map.of("name", "marko", "c", 1L),
                Map.of("name", "josh", "c", 3L)
        );
        assertEquals(gt0expected, gt0);

        Set<Map<Object, Object>> gt1 = V().as("v").local(out().count()).is(gt(1)).as("c")
                .select("v", "c").values("name:/v/name", "c").toSet();
        Set<Map<String, Object>> gt1expected = Set.of(
                Map.of("name", "josh", "c", 3L)
        );
        assertEquals(gt1expected, gt1);

        Set<Map<Object, Object>> lte2 = V().as("v").local(out().count()).is(lte(2)).as("c")
                .select("v", "c").values("name:/v/name", "c").toSet();
        Set<Map<String, Object>> lte2expected = Set.of(
                Map.of("name", "root", "c", 1L),
                Map.of("name", "marko", "c", 1L),
                Map.of("name", "lop", "c", 0L),
                Map.of("name", "ripple", "c", 0L),
                Map.of("name", "linux", "c", 0L)
        );
        assertEquals(lte2expected, lte2);

        Set<Map<Object, Object>> lte1 = V().as("v").local(out().count()).is(lte(1)).as("c")
                .select("v", "c").values("name:/v/name", "c").toSet();
        Set<Map<String, Object>> lte1expected = Set.of(
                Map.of("name", "root", "c", 1L),
                Map.of("name", "marko", "c", 1L),
                Map.of("name", "lop", "c", 0L),
                Map.of("name", "ripple", "c", 0L),
                Map.of("name", "linux", "c", 0L)
        );
        assertEquals(lte1expected, lte1);

        Set<Map<Object, Object>> lt1 = V().as("v").local(out().count()).is(lt(1)).as("c")
                .select("v", "c").values("name:/v/name", "c").toSet();
        Set<Map<String, Object>> lt1expected = Set.of(
                Map.of("name", "lop", "c", 0L),
                Map.of("name", "ripple", "c", 0L),
                Map.of("name", "linux", "c", 0L)
        );
        assertEquals(lt1expected, lt1);

    }

}
