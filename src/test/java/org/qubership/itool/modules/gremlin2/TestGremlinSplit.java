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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

public class TestGremlinSplit extends AbstractGremlinTest {

    @BeforeEach
    @Override
    public void cleanup() {
        super.cleanup();
        JsonObject json = new JsonObject();
        json.put("id", "v10").put("type", "domain")
            .put("content", "Some multiline\nContent\r\n====\n\rLast line")
            .put("database", "postgresql, mongodb,oracle");
        this.graph.addVertexUnderRoot(json);
    }

    @Test
    void test_Split() {
        List<String> result = V("v10").value("content").<String>split().toList();
//        print(result);
        Assertions.assertEquals(4, result.size());
        Assertions.assertEquals("Some multiline", result.get(0));
        Assertions.assertEquals("Content", result.get(1));
        Assertions.assertEquals("====", result.get(2));
        Assertions.assertEquals("Last line", result.get(3));
    }

    @Test
    void test_SplitBy() {
        List<String> result = V("v10").value("database").<String>split().by(",\\s*").toList();
//        print(result);
        Assertions.assertEquals(3, result.size());
        Assertions.assertEquals("postgresql", result.get(0));
        Assertions.assertEquals("mongodb", result.get(1));
        Assertions.assertEquals("oracle", result.get(2));
    }

}
