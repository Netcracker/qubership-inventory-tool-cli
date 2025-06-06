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

import static org.qubership.itool.modules.gremlin2.P.neq;
import static org.qubership.itool.modules.gremlin2.graph.__.*;

public class TestGremlinRepeat extends AbstractGremlinTest {

    @Override
    protected void createGraph() {
        createComplexGraph();
    }

    @Test
    void test_repeat() {
        List<Path> result = V("v1").repeat(out()).times(2)
            .path().by("name").toList();
//        System.out.println(result);
        // ==>[marko,josh,ripple]
        // ==>[marko,josh,lop]
        Assertions.assertEquals(2, result.size());
        assertPath(result.get(0), "marko", "josh", "ripple");
        assertPath(result.get(1), "marko", "josh", "lop");
    }

    @Test
    void test_until_repeat() {
        List<Path> result = V().hasNotId("root")
            .until(has("name", "ripple")).repeat(out())
            .path().by("name").toList();
//        System.out.println(result);
        //==>[marko,josh,ripple]
        //==>[josh,ripple]
        //==>[ripple]
        Assertions.assertEquals(3, result.size());
        assertPath(result.get(0), "marko", "josh", "ripple");
        assertPath(result.get(1), "josh", "ripple");
        assertPath(result.get(2), "ripple");
    }

    @Test
    void test_repeat_until_same() {
        List<Path> result = V().hasNotId("root")
            .repeat(out())
            .until(has("name", "ripple"))
            .path().by("name").toList();
//        System.out.println(result);
        //==>[marko,josh,ripple]
        //==>[josh,ripple]
        Assertions.assertEquals(2, result.size());
        assertPath(result.get(0), "marko", "josh", "ripple");
        assertPath(result.get(1), "josh", "ripple");
    }

    @Test
    void test_repeat_until() {
        List<Path> result = V("v1").repeat(out()).until(hasType("software"))
            .path().by("name").toList();
//        System.out.println(result);
        //==>[marko,lop]
        //==>[marko,josh,ripple]
        //==>[marko,josh,lop]
        Assertions.assertEquals(3, result.size());
        assertPath(result.get(0), "marko", "lop");
        assertPath(result.get(1), "marko", "josh", "ripple");
        assertPath(result.get(2), "marko", "josh", "lop");
    }

    @Test
    void test_repeat_until_count() {
        List<Path> result = V("v1").repeat(out()).until(outE().count().is(0)).path().by("name").toList();
//        System.out.println(result);
        //==>[marko,vadas]
        //==>[marko,lop]
        //==>[marko,josh,ripple]
        //==>[marko,josh,lop]
        Assertions.assertEquals(4, result.size());
        assertPath(result.get(0), "marko", "vadas");
        assertPath(result.get(1), "marko", "lop");
        assertPath(result.get(2), "marko", "josh", "ripple");
        assertPath(result.get(3), "marko", "josh", "lop");
    }

    @Test
    void test_repeat_times_emit() {
        List<Path> result = V("v1").repeat(out()).times(2).emit().path().by("name").toList();
//        System.out.println(result);
        //==>[marko,vadas]
        //==>[marko,josh]
        //==>[marko,lop]
        //==>[marko,josh,ripple]
        //==>[marko,josh,lop]
        Assertions.assertEquals(5, result.size());
        assertPath(result.get(0), "marko", "vadas");
        assertPath(result.get(1), "marko", "josh");
        assertPath(result.get(2), "marko", "lop");
        assertPath(result.get(3), "marko", "josh", "ripple");
        assertPath(result.get(4), "marko", "josh", "lop");
    }

    @Test
    void test_emit_repeat_times() {
        List<Path> result = V("v1").emit().repeat(out()).times(2).path().by("name").toList();
//        System.out.println(result);
        //==>[marko]
        //==>[marko,vadas]
        //==>[marko,josh]
        //==>[marko,lop]
        //==>[marko,josh,ripple]
        //==>[marko,josh,lop]
        Assertions.assertEquals(6, result.size());
        assertPath(result.get(0), "marko");
        assertPath(result.get(1), "marko", "vadas");
        assertPath(result.get(2), "marko", "josh");
        assertPath(result.get(3), "marko", "lop");
        assertPath(result.get(4), "marko", "josh", "ripple");
        assertPath(result.get(5), "marko", "josh", "lop");
    }

    @Test
    void test_repeat_times_emit_has() {
        List<Path> result = V("v1").repeat(out()).times(2).emit(hasKey("lang")).path().by("name").toList();
//        System.out.println(result);
        //==>[marko,lop]
        //==>[marko,josh,ripple]
        //==>[marko,josh,lop]
        Assertions.assertEquals(3, result.size());
        assertPath(result.get(0), "marko", "lop");
        assertPath(result.get(1), "marko", "josh", "ripple");
        assertPath(result.get(2), "marko", "josh", "lop");
    }

    @Test
    void test_repeat_until_repeat_emit() {
        List<JsonObject> result = V("v1")
            .repeat(out("knows"))
            .until(repeat(out("created"))
            .emit(has("name", "lop"))).toList();
//        System.out.println(result);
        //==>[v4]
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("v4", result.get(0).getString("id"));
    }

    @Test
    void test_emit_has_repeat() {
        List<Path> result = V("v1").emit(hasType("person")).repeat(out()).path().by("name").toList();
//        System.out.println(result);
        //==>[marko]
        //==>[marko,vadas]
        //==>[marko,josh]
        Assertions.assertEquals(3, result.size());
        assertPath(result.get(0), "marko");
        assertPath(result.get(1), "marko", "vadas");
        assertPath(result.get(2), "marko", "josh");
    }

    @Test
    void test_DOMAIN2_simple_BACKEND2() {
        graph.clear();
        createLoopedGraph();

        List<String> result = V("DOMAIN2-BACKEND2").as("C")
            .repeat(out("module", "dependence"))
            .until(in("module").where(neq("C")))
            .in("module")
            .id().toList();
//        System.out.println(result);
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("DOMAIN2-BACKEND1", result.get(0));
    }

    @Test
    void test_DOMAIN2_simple_BACKEND1() {
        graph.clear();
        createLoopedGraph();

        List<String> result = V("DOMAIN2-BACKEND1").as("C")
            .repeat(out("module", "dependence"))
            .until(in("module").where(neq("C")))
            .in("module")
            .id().toList();
//        System.out.println(result);
        Assertions.assertEquals(2, result.size());
        Assertions.assertEquals("DOMAIN2-LIB", result.get(0));
        Assertions.assertEquals("DOMAIN2-LIB", result.get(1));
    }

    @Test
    void test_DOMAIN2_simple_DOMAIN2LIB() {
        graph.clear();
        createLoopedGraph();

        List<String> result = V("DOMAIN2-LIB").as("C")
            .repeat(out("module", "dependence"))
            .until(in("module").where(neq("C")))
            .in("module")
            .id().toList();
//        System.out.println(result);
        Assertions.assertEquals(0, result.size());
    }

    @Test
    void test_DOMAIN2LIB_all() {
        graph.clear();
        createLoopedGraph();

        List<String> result = V("DOMAIN2-LIB").out("module")
            .repeat(out()).emit()
            .dedup()
            .id().toList();
//        System.out.println(result);
        // [LIB1_1, LIB2, LIB1, LIB2_1, DOMAIN2-LIB-M1]
        Assertions.assertEquals(5, result.size());
        Assertions.assertTrue(result.contains("LIB1_1"));
        Assertions.assertTrue(result.contains("LIB2"));
        Assertions.assertTrue(result.contains("LIB1"));
        Assertions.assertTrue(result.contains("LIB2_1"));
        Assertions.assertTrue(result.contains("DOMAIN2-LIB-M1"));
    }

    @Test
    void test_DOMAIN2LIB_direct() {
        graph.clear();
        createLoopedGraph();

        List<String> result = V("DOMAIN2-LIB").out("module")
            .out()
            .dedup()
            .id().toList();
//        System.out.println(result);
        // [LIB1_1]
        Assertions.assertEquals(1, result.size());
        Assertions.assertTrue(result.contains("LIB1_1"));
    }

    @Test
    void test_DOMAIN2BE1_transitive() {
        graph.clear();
        createLoopedGraph();

        List<String> result = V("DOMAIN2-BACKEND1").as("C").out("module")
            .out("dependence")
            .repeat(out("dependence").as("R")).emit()
            .dedup()
            .id().toList();
        System.out.println(result);
        // LIB1, LIB2_1, LIB1_1, DOMAIN2-LIB-M1, LIB2, DOMAIN2-LIB-M2
        Assertions.assertEquals(6, result.size());
        Assertions.assertTrue(result.contains("LIB1"));
        Assertions.assertTrue(result.contains("LIB2_1"));
        Assertions.assertTrue(result.contains("LIB1_1"));
        Assertions.assertTrue(result.contains("DOMAIN2-LIB-M1"));
        Assertions.assertTrue(result.contains("LIB2"));
        Assertions.assertTrue(result.contains("DOMAIN2-LIB-M2"));
    }

}
