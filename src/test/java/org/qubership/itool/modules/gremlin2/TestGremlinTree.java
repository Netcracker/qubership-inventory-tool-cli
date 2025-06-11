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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.qubership.itool.modules.gremlin2.step.util.Tree;

public class TestGremlinTree extends AbstractGremlinTest {

    @Test
    void test_plain() {
        Tree<String> tree = new Tree<>();
        tree.add("A");
        tree.add("B");
//        System.out.println(tree);
        Assertions.assertEquals(2, tree.size());
    }

    @Test
    void test_level_1() {
        Tree<String> tree = new Tree<>();
        tree.add("A").add("A1").add(("A2"));
        tree.add("B");
//        System.out.println(tree);
        Assertions.assertEquals(2, tree.size());
        Assertions.assertEquals(1, tree.get("A").size());
        Assertions.assertEquals(1, tree.get("A").get("A1").size());
    }

    @Test
    void test_level_n() {
        Tree<String> tree = new Tree<>();
        tree.add("A").add("A1").add(("A2"));
        Tree<String> b2Tree = tree.add("B").add("B2");
        b2Tree.add("B2_1");
        b2Tree.add("B2_2");
//        System.out.println(tree);
        Assertions.assertEquals(2, tree.size());
        Assertions.assertEquals(1, tree.get("A").size());
        Assertions.assertEquals(1, tree.get("A").get("A1").size());
        Assertions.assertEquals(1, tree.get("B").size());
        Assertions.assertEquals(2, tree.get("B").get("B2").size());
    }
    @Test
    @Disabled
    void test_level_z() {
        Tree<String> tree = new Tree<>();
        tree.add("A").add("A1").add(("A2"));
        Tree bTree = tree.add("B");
        Tree<String> b2Tree = bTree.add("B2");
        Tree b211Tree = b2Tree.add("B2_1").add("B2_1_1");
        b211Tree.add("B2_1_1_1");
        b211Tree.add("B2_1_1_2");
        b211Tree.add("B2_1_1_3");
        b2Tree.add("B2_2");
        bTree.add("B3");
//        System.out.println(tree);
    }

    @Test
    void test_tree() {
        Tree<JsonObject> tree = V().hasNotId("root").out().out().tree().next();
//        System.out.println(tree);
        Assertions.assertEquals(1, tree.size());
        Assertions.assertEquals(1, tree.get(graph.getVertex("v1")).size());
        Assertions.assertEquals(3, tree.get(graph.getVertex("v1")).get(graph.getVertex("v4")).size());
    }

    @Test
    void test_tree_by() {
        Tree<String> tree = V().hasNotId("root").out().out().tree().by("name").next();
//        System.out.println(tree);
        Assertions.assertEquals(1, tree.size());
        Assertions.assertEquals(1, tree.get("marko").size());
        Assertions.assertEquals(3, tree.get("marko").get("josh").size());
    }

}
