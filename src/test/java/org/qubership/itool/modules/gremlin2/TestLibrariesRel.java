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
import org.qubership.itool.modules.graph.GraphDumpSupport;
import org.qubership.itool.modules.graph.GraphImpl;
import org.qubership.itool.modules.gremlin2.graph.GraphTraversal;
import org.qubership.itool.modules.gremlin2.graph.GraphTraversalSource;
import org.qubership.itool.modules.gremlin2.structure.MapElement;
import org.qubership.itool.utils.JsonUtils;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import org.junit.jupiter.api.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.qubership.itool.modules.gremlin2.P.*;
import static org.qubership.itool.modules.gremlin2.graph.__.has;
import static org.qubership.itool.modules.gremlin2.graph.__.out;
import static org.qubership.itool.modules.gremlin2.graph.__.outE;
import static org.qubership.itool.modules.gremlin2.graph.__.select;
import static org.qubership.itool.modules.gremlin2.graph.__.value;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Disabled("Refers to task.result.json that is currently missing")
public class TestLibrariesRel {

    protected Graph graph;
    protected GraphTraversalSource graphTraversalSource;

    @BeforeAll
    public void setup() {
        this.graph = new GraphImpl();
        this.graphTraversalSource = new GraphTraversalSource(this.graph);
    }

    @BeforeEach
    public void cleanup() throws IOException {
        this.graph.clear();
        JsonObject dump = JsonUtils.readJsonResource(getClass(), "task.result.json");
        GraphDumpSupport.restoreFromJson(this.graph, dump);
    }

    protected GraphTraversal<JsonObject, JsonObject> V(final String... vertexIds) {
        return this.graphTraversalSource.V(vertexIds);
    }

    protected GraphTraversal<JsonObject, JsonObject> E(final String... edgeIds) {
        return this.graphTraversalSource.E(edgeIds);
    }

    @Test
    void test_outE() {
        long startTime = System.currentTimeMillis();
        Long count = V().hasType("domain").has("department", eq("DEPARTMENT2")).as("D")
            .out().outE().count().next();
        long endTime = System.currentTimeMillis();
        System.out.println(count + " // " + (endTime - startTime));
    }

    @Test
    void test_outE_inV() {
        long startTime = System.currentTimeMillis();
        Long count = V().hasType("domain").has("department", eq("DEPARTMENT2")).as("D")
            .out().outE().inV()
            .count().next();
        long endTime = System.currentTimeMillis();
        System.out.println(count + " // " + (endTime - startTime));
    }

    @Test
    void test_repeat() {
        long startTime = System.currentTimeMillis();
        List<String> result = V("COMPONENT7").out("module")
            .repeat(out("dependency")).times(2).emit()
            .dedup()
            .id()
            .toList();
        long endTime = System.currentTimeMillis();
        System.out.println("Size: " + result.size() + " // " + (endTime - startTime));
    }

    @Test
    void test() {
        List<Map<Object, Object>> result =
            V("org.springframework.kafka:spring-kafka:2.6.7")
                .outE().values("id", "type", "scope").as("E")
                .inV().values("id", "package").as("L")
                .select("E", "L")
                .group().by("L").by("E")
                .toList();
        System.out.println(result);
    }

    @Test
    @Disabled
    void test_s() {
        List<JsonObject> result = V("COMPONENT8").as("C").out("module").out("dependency").dedup()
            .repeat(
                outE("dependency").has("component", eq(select("C").id())).inV().dedup()
            ).emit()
            .order().by("id").toList();
        System.out.println(result);
    }

    @Test
    @Disabled
    void test_s1() {
        List<Object> result = V("COMPONENT7").as("C").out("module").out("dependency").dedup()
            .repeat(outE("dependency").has("component", eq(select("C").id())).inV().dedup())
            .emit().dedup()
            .values("groupId", "version").dedup()
            .group().by("groupId").by("version")
            .unfold().by(MapElement.both)
            .where(value().size().is(gte(1)))
            .toList();
        System.out.println(result);

    }

    @Test
    @Disabled
    void test_s2() {
        List<String> result = V("COMPONENT7").as("C")
            .where(
                out("module").outE("dependency").has("scope", neq("test")).inV().dedup()
                    .outE("dependency").has("scope", neq("test")).inV().dedup()
                    .repeat(
                        outE("dependency")
                            .has("scope", neq("test"))
                            .has("component", eq(select("C").id()))
                            .inV().dedup()
                    ).emit().dedup()
                    .where(has("id", containing("censored-censored-common")).has("version", gteVersion("2021.2.1.0")))
            )
            .id().toList();

        System.out.println(result);
    }

    @Test
    @Disabled
    void test_s3() {
        List<String> result = V("COMPONENT7").as("C")
            .out("module").outE("dependency").has("scope", neq("test")).inV().dedup()
            .outE("dependency").has("scope", neq("test")).inV().dedup()
            .repeat(
                outE("dependency")
                    .has("scope", neq("test"))
                    .has("component", eq(select("C").id()))
                    .inV().dedup()
            ).emit().dedup()
            .where(has("id", containing("censored-censored-common")).has("version", gteVersion("2021.2.1.0")))
            .select("C").id().toList();

        System.out.println(result);
    }

    @Test
    @Disabled
    void test_s4() {
        Map<Object, Object> result = V().hasType("domain").out().as("C")
            .out("module").outE("dependency")
            .has("scope", neq("test"))
            .has("component", eq(select("C").id()))
            .inV()
            .or(
                has("groupId", eq("org.springframework.boot"))
                , has("groupId", eq("io.quarkus"))
            ).as("F")
            .select("C", "F")
            .<String>values("/C/id", "/F/groupId", "/F/version").dedup()
            .group().by("id").next();
        System.out.println(result);
    }

    @Test
    @Disabled
    void test_s5() {
        Map<String, List<String>> result = V().hasType("domain").out().hasType("library").id().as("L")
            .out("module").<String>value("artifactId").as("A")
            .<String>select("L", "A")
            .<String, List<String>>group().by("L").by("A").next();
        System.out.println(result);
    }

    @Test
    @Disabled
    void testInfra() {
        List<JsonObject> result = V("Infrastructure").value("env").unfold().by(MapElement.value).value("items").<JsonObject>unfold().toList();
        System.out.println(result);
        for (JsonObject json : result) {
            System.out.println("#  " + json);
        }
    }

    @Test
    @Disabled
    void testFromDependency() {
        String componentId = "COMPONENT7";
        List<JsonObject> dependencies = new ArrayList<>();

        // directDependencies ================================
        GraphTraversal<JsonObject, JsonObject> directTraversal =
            V(componentId).as("C").out("module")
                .outE("dependency")
                .has("scope", neq("test"))
                .has("component", eq(select("C").id()))
                .inV().dedup();
        dependencies.addAll(directTraversal.clone().toList());

        // transitiveDependencies ============================
        List<JsonObject> transitiveDependencies =
            directTraversal.clone()
                .outE("dependency").has("scope", neq("test")).inV().dedup()
                .repeat(
                    outE("dependency")
                        .has("scope", neq("test"))
                        .has("component", eq(select("C").id()))
                        .inV().dedup()
                ).emit().dedup().toList();
        dependencies.addAll(transitiveDependencies);

        List<JsonObject> infraVertexes = V("Infra").out().toList();

        for (JsonObject vertex : infraVertexes) {
            JsonArray drivers = vertex.getJsonArray("drivers");
            if (drivers == null || drivers.isEmpty()) {
                continue;
            }
            for (Object tmp : drivers) {
                JsonObject driver = (JsonObject) tmp;
                String driverGroupId = driver.getString("groupId");
                String driverArtifactId = driver.getString("artifactId");

                for (JsonObject dependency : dependencies) {
                    String dependencyGroupId = dependency.getString("groupId");
                    String dependencyArtifactId = dependency.getString("artifactId");
                    System.out.println("groupId: " + dependencyGroupId + ", artifactId: " + dependencyArtifactId);
                    if (driverGroupId != null && driverGroupId.equals(dependencyGroupId)) {
                        if (driverArtifactId != null && driverArtifactId.equals(dependencyArtifactId)) {
                            System.out.println("FOUND: groupId: " + driverGroupId + ", artifactId: " + driverArtifactId);
                        }
                    }
                }
            }
        }
    }
}
