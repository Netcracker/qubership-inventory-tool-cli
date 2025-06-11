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

import org.qubership.itool.modules.gremlin2.graph.__;

import java.util.*;
import java.util.stream.Collectors;

public class TestGremlinGlob extends AbstractGremlinTest {
    private int edgeSeq = 0;

    @Override
    protected void createSimpleGraph() {
        /*
        Graph structure:
        ROOT
            COMPONENT (C)
                - Dockerfile (v1)
                - pom.xml (v2)
                - config.yaml (v11)
                + deployments/ (v3)
                    - routes-configuration.yaml (v4)
                    - default.json (v5)
                    + charts/ (v6)
                        + microservice/ (v7)
                            - Chart.yaml (v8)
                            - values.yaml (v9)
                            - values.schema.json (v10)
         */
        JsonObject v_c = new JsonObject().put("id", "C").put("name", "COMPONENT");

        JsonObject v1_Dockerfile = createFileVertex(1, "Dockerfile");
        JsonObject v2_pom_xml = createFileVertex(2, "pom.xml");
        JsonObject v3_deployments = createDirectoryVertex(3, "deployments");
        JsonObject v4_routes_configuration_yaml = createFileVertex(4, "routes-configuration.yaml");
        JsonObject v5_default_json = createFileVertex(5, "default.json");
        JsonObject v6_charts = createDirectoryVertex(6, "charts");
        JsonObject v7_microservice = createDirectoryVertex(7, "microservice");
        JsonObject v8_Chart_yaml = createFileVertex(8, "Chart.yaml");
        JsonObject v9_values_yaml = createFileVertex(9, "values.yaml");
        JsonObject v10_values_schema_json = createFileVertex(10, "values.schema.json");
        JsonObject v11_config_yaml = createFileVertex(11, "config.yaml");

        this.graph.addVertexUnderRoot(v_c);
        this.graph.addEdge(v_c, v1_Dockerfile, createFileEdge());
        this.graph.addEdge(v_c, v2_pom_xml, createFileEdge());
        this.graph.addEdge(v_c, v3_deployments, createDirectoryEdge());
        this.graph.addEdge(v_c, v11_config_yaml, createDirectoryEdge());
        this.graph.addEdge(v3_deployments, v4_routes_configuration_yaml, createFileEdge());
        this.graph.addEdge(v3_deployments, v5_default_json, createFileEdge());
        this.graph.addEdge(v3_deployments, v6_charts, createDirectoryEdge());
        this.graph.addEdge(v6_charts, v7_microservice, createDirectoryEdge());
        this.graph.addEdge(v7_microservice, v8_Chart_yaml, createFileEdge());
        this.graph.addEdge(v7_microservice, v9_values_yaml, createFileEdge());
        this.graph.addEdge(v7_microservice, v10_values_schema_json, createFileEdge());
    }

    private JsonObject createFileVertex(int id, String name) {
        return new JsonObject().put("id", "v" + id).put("name", name).put("type", "file");
    }

    private JsonObject createDirectoryVertex(int id, String name) {
        return new JsonObject().put("id", "v" + id).put("name", name).put("type", "directory");
    }

    private JsonObject createFileEdge() {
        return new JsonObject().put("id", "e" + ++edgeSeq).put("type", "file");
    }

    private JsonObject createDirectoryEdge() {
        return new JsonObject().put("id", "e" + ++edgeSeq).put("type", "directory");
    }

    private List<String> collectFileNamesList(List<JsonObject> result) {
        return result.stream().map(vertex -> vertex.getString("name")).collect(Collectors.toList());
    }

    private <T> void assertContentsEqual(Set<T> expected, List<T> actual) {
        Assertions.assertEquals(expected.size(), actual.size());
        Assertions.assertEquals(expected, new HashSet<>(actual));
    }


    @Test
    public void testSimplePattern() {
        List<JsonObject> result = V("C").out().glob("pom.xml").toList();

        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.size());
        JsonObject vertex = result.get(0);
        Assertions.assertEquals("pom.xml", vertex.getString("name"));
    }

    @Test
    public void testWrongSimplePattern() {
        List<JsonObject> result = V("C").out().glob("pom.xmll").toList();

        Assertions.assertNotNull(result);
        Assertions.assertEquals(0, result.size());
    }

    @Test
    void testSimplePatternByName() {
        List<JsonObject> result = V("C").out().glob("pom.xml").by("name").toList();

        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.size());
        JsonObject vertex = result.get(0);
        Assertions.assertEquals("pom.xml", vertex.getString("name"));
    }

    @Test
    void testSimplePatternById() {
        List<JsonObject> result = V("C").out().glob("pom.xml").by("id").toList();

        Assertions.assertNotNull(result);
        Assertions.assertEquals(0, result.size());
    }

    @Test
    void testSingleCharacterPattern() {
        List<JsonObject> result = V("C").out().glob("pom.xm?").toList();

        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.size());
        JsonObject vertex = result.get(0);
        Assertions.assertEquals("pom.xml", vertex.getString("name"));
    }

    @Test
    void testZeroOrMorePattern() {
        List<JsonObject> result = V("C").out().glob("*.xml").toList();

        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.size());
        JsonObject vertex = result.get(0);
        Assertions.assertEquals("pom.xml", vertex.getString("name"));
    }

    @Test
    void testZeroOrMoreAndSingleCharacterPattern() {
        List<JsonObject> result = V("C").out().glob("*.xm?").toList();

        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.size());
        JsonObject vertex = result.get(0);
        Assertions.assertEquals("pom.xml", vertex.getString("name"));
    }

    @Test
    void testRecursiveWildcardPattern() {
        List<JsonObject> result = V("C").out().glob("**/Chart.yaml").toList();

        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.size());
        JsonObject vertex = result.get(0);
        Assertions.assertEquals("Chart.yaml", vertex.getString("name"));
    }

    @Test
    public void testRecursiveWildcardWithZeroOrMorePattern() {
        List<JsonObject> result = V("C").out().glob("**/*.yaml").toList();

        Assertions.assertNotNull(result);
        assertContentsEqual(
            Set.of("config.yaml", "routes-configuration.yaml", "Chart.yaml", "values.yaml"),
            collectFileNamesList(result)
        );
        Assertions.assertEquals(4, result.size());
    }

    @Test
    public void testDoubleRecursiveWildcardWithStaticPattern() {
        List<JsonObject> result = V("C").out().glob("**/charts/**/Chart.yaml").toList();

        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.size());
        JsonObject vertex = result.get(0);
        Assertions.assertEquals("Chart.yaml", vertex.getString("name"));
    }

    @Test
    public void testFirstRecursiveWildcardWithStaticPattern() {
        List<JsonObject> result = V("C").out().glob("**/charts/*/Chart.yaml").toList();

        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.size());
        JsonObject vertex = result.get(0);
        Assertions.assertEquals("Chart.yaml", vertex.getString("name"));
    }

    @Test
    public void testSecondRecursiveWildcardWithStaticPattern() {
        List<JsonObject> result = V("C").out().glob("*/charts/**/Chart.yaml").toList();

        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.size());
        JsonObject vertex = result.get(0);
        Assertions.assertEquals("Chart.yaml", vertex.getString("name"));
    }

    @Test
    public void testWildcardOnlyPattern() {
        List<Map<String, List<JsonObject>>> results = V("C").out().glob("*")
            .<String, List<JsonObject>>group().by("type").toList();

        Assertions.assertEquals(1, results.size());
        Map<String, List<JsonObject>> result = results.get(0);

        Assertions.assertEquals(Set.of("file", "directory"), result.keySet());

        assertContentsEqual(
            Set.of("Dockerfile", "pom.xml", "config.yaml"),
            collectFileNamesList(result.get("file"))
        );

        assertContentsEqual(
            Set.of("deployments"),
            collectFileNamesList(result.get("directory"))
        );
    }

    @Test
    public void testRecursiveWildcardOnlyPattern() {
        List<Map<String, List<JsonObject>>> results = V("C").out().glob("**/**")
            .<String, List<JsonObject>>group().by("type").toList();

        Assertions.assertEquals(1, results.size());
        Map<String, List<JsonObject>> result = results.get(0);

        Assertions.assertEquals(Set.of("file", "directory"), result.keySet());

        assertContentsEqual(
            Set.of("Dockerfile", "pom.xml", "config.yaml", "routes-configuration.yaml", "default.json",
                    "Chart.yaml", "values.yaml", "values.schema.json"),
            collectFileNamesList(result.get("file"))
        );

        assertContentsEqual(
            Set.of("deployments", "charts", "microservice"),
            collectFileNamesList(result.get("directory"))
        );
    }

    @Test
    public void testRepetitiveRecursiveWildcardOnlyPattern() {
        List<Map<String, List<JsonObject>>> results = V("C").out().glob("/**/**/**/*")
            .<String, List<JsonObject>>group().by("type").toList();

        Assertions.assertEquals(1, results.size());
        Map<String, List<JsonObject>> result = results.get(0);

        Assertions.assertEquals(Set.of("file", "directory"), result.keySet());

        assertContentsEqual(
            Set.of("Dockerfile", "pom.xml", "config.yaml", "routes-configuration.yaml", "default.json",
                "Chart.yaml", "values.yaml", "values.schema.json"),
            collectFileNamesList(result.get("file"))
        );

        assertContentsEqual(
            Set.of("deployments", "charts", "microservice"),
            collectFileNamesList(result.get("directory"))
        );
    }

    @Test
    public void testTrailingZeroOrMorePattern() {
        List<JsonObject> result = V("C").out().glob("deployments/charts/microservice/*.yaml").toList();

        Assertions.assertNotNull(result);
        assertContentsEqual(
            Set.of("Chart.yaml", "values.yaml"),
            collectFileNamesList(result)
        );
        Assertions.assertEquals(2, result.size());
    }

    @Test
    public void testTrailingSimpleWildcardPattern() {
        List<JsonObject> result = V("C").out().glob("deployments/charts/microservice/*").toList();

        Assertions.assertNotNull(result);
        assertContentsEqual(
            Set.of("Chart.yaml", "values.yaml", "values.schema.json"),
            collectFileNamesList(result)
        );
        Assertions.assertEquals(3, result.size());
    }

    @Test
    public void testTrailingRecursiveWildcardPattern() {
        List<JsonObject> result = V("C").out().glob("deployments/charts/microservice/**").toList();

        Assertions.assertNotNull(result);
        assertContentsEqual(
            Set.of("Chart.yaml", "values.yaml", "values.schema.json"),
            collectFileNamesList(result)
        );
        Assertions.assertEquals(3, result.size());
    }

    @Test
    void testRecursiveWildcardWithStaticPattern() {
        List<JsonObject> result = V("C").out().glob("**/microservice/Chart.yaml").toList();

        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.size());
        JsonObject vertex = result.get(0);
        Assertions.assertEquals("Chart.yaml", vertex.getString("name"));
    }

    @Test
    void testRecursiveWildcardWithSingleCharacterPattern() {
        List<JsonObject> result = V("C").out().glob("**/microservice/Chart.yam?").toList();

        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.size());
        JsonObject vertex = result.get(0);
        Assertions.assertEquals("Chart.yaml", vertex.getString("name"));
    }

    @Test
    void testRecursiveWildcardWithDoubleSingleCharacterPattern() {
        List<JsonObject> result = V("C").out().glob("**/microservic?/Chart.yam?").toList();

        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.size());
        JsonObject vertex = result.get(0);
        Assertions.assertEquals("Chart.yaml", vertex.getString("name"));
    }

    @Test
    void testRecursiveWildcardWithStaticWithZeroOrMorePattern() {
        List<JsonObject> result = V("C").out().glob("**/charts/*/Chart.yaml").toList();

        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.size());
        JsonObject vertex = result.get(0);
        Assertions.assertEquals("Chart.yaml", vertex.getString("name"));
    }

    @Test
    void testWrongRecursiveWildcardWithZeroOrMorePattern() {
        List<JsonObject> result = V("C").out().glob("**/charts/*/pom.xml").toList();

        Assertions.assertNotNull(result);
        Assertions.assertEquals(0, result.size());
    }

    @Test
    void testZeroOrMoreWithStaticFilePattern() {
        List<JsonObject> result = V("C").out().glob("*/default.json").toList();

        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.size());
        JsonObject vertex = result.get(0);
        Assertions.assertEquals("default.json", vertex.getString("name"));
    }

    @Test
    void testByTraversalModulate() {
        List<JsonObject> result = V("C").out()
            .glob("*/default.json")
            .by(__.out("directory", "file").hasType("directory", "file"))
            .toList();

        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.size());
        JsonObject vertex = result.get(0);
        Assertions.assertEquals("default.json", vertex.getString("name"));
    }

    @Test
    void testWrongByTraversalModulate() {
        List<JsonObject> result = V("C").out()
            .glob("*/default.json")
            .by(__.out("directory", "file").hasType("s"))
            .toList();

        Assertions.assertNotNull(result);
        Assertions.assertEquals(0, result.size());
    }

}
