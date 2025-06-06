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

import org.qubership.itool.modules.graph.Graph;
import org.qubership.itool.modules.graph.GraphImpl;
import org.qubership.itool.modules.query.QueryExecutor;
import org.qubership.itool.modules.query.converter.ToJsonArrayConverter;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestToJsonArrayConverter {

    private QueryExecutor queryConverter;

    @BeforeAll
    public void setup() {
        Graph graph = new GraphImpl();

        this.queryConverter = new QueryExecutor(graph);
        this.queryConverter.setConverter(new ToJsonArrayConverter());

        JsonObject root = graph.getVertex("root");
        JsonObject domain1 = new JsonObject().put("id", "D1");
        JsonObject component1 = new JsonObject().put("id", "C1");
        graph.addVertex(root, domain1);
        graph.addVertex(domain1, component1);
    }

    @Test
    public void jsonObjectTest() {
        Object result = this.queryConverter.executeAndConvert(".V().toList();");
        Assertions.assertEquals(
            new JsonArray()
                .add(Map.of("id", "root", "type", "root", "name", "root"))
                .add(Map.of("id", "D1"))
                .add(Map.of("id", "C1")),
            result);
    }

    @Test
    public void mapTest() {
        Object result = this.queryConverter.executeAndConvert(".V().values(\"id\").toList();");
        Assertions.assertEquals(
            new JsonArray()
                .add(new JsonObject().put("id", "root"))
                .add(new JsonObject().put("id", "D1"))
                .add(new JsonObject().put("id", "C1")),
            result);
    }

}
