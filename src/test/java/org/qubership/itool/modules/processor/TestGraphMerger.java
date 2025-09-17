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

package org.qubership.itool.modules.processor;

import org.qubership.itool.modules.gremlin2.P;
import org.junit.jupiter.api.*;

import org.qubership.itool.modules.graph.FalloutDto;
import org.qubership.itool.modules.graph.Graph;
import org.qubership.itool.modules.graph.GraphDumpSupport;
import org.qubership.itool.modules.gremlin2.graph.__;
import org.qubership.itool.modules.processor.tasks.CreateAppVertexTask;
import org.qubership.itool.modules.report.GraphReport;
import org.qubership.itool.modules.report.GraphReportImpl;
import org.qubership.itool.utils.JsonUtils;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;
import jakarta.inject.Provider;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.qubership.itool.modules.graph.Graph.F_ID;
import static org.qubership.itool.modules.graph.Graph.F_MICROSERVICE_FLAG;
import static org.qubership.itool.modules.graph.Graph.F_MOCK_FLAG;
import static org.qubership.itool.modules.graph.Graph.F_NAME;
import static org.qubership.itool.modules.graph.Graph.F_REPOSITORY;
import static org.qubership.itool.modules.graph.Graph.F_TYPE;
import static org.qubership.itool.modules.graph.Graph.F_VERSION;
import static org.qubership.itool.modules.graph.Graph.P_DETAILS_DNS_NAME;
import static org.qubership.itool.modules.graph.Graph.P_DETAILS_DNS_NAMES;
import static org.qubership.itool.modules.graph.Graph.V_APPLICATION;
import static org.qubership.itool.modules.graph.Graph.V_DOMAIN;
import static org.qubership.itool.modules.graph.Graph.V_ROOT;
import static org.qubership.itool.modules.processor.MergerApi.P_APP_NAME;
import static org.qubership.itool.modules.processor.MergerApi.P_APP_VERSION;
import static org.qubership.itool.modules.processor.MergerApi.P_FILE_NAME;
import static org.qubership.itool.modules.processor.MergerApi.P_IS_APPLICATION;
import static org.qubership.itool.modules.processor.MergerApi.P_IS_NAMESPACE;
import static org.qubership.itool.modules.processor.MergerApi.P_NAMESPACE_NAME;

import io.vertx.core.Vertx;
import org.qubership.itool.modules.graph.GraphImpl;
import org.qubership.itool.di.ApplicationContext;
import org.qubership.itool.di.QubershipModule;
import com.google.inject.Module;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestGraphMerger {

    private Vertx vertx;
    private Provider<Graph> graphProvider;
    private Provider<MergerApi> graphMergerProvider;

    @BeforeAll
    public void setup() {
        vertx = Vertx.vertx();
        JsonObject config = new JsonObject();
        ApplicationContext context = new ApplicationContext(vertx, config, new Module[] {new QubershipModule(vertx)});
        graphMergerProvider = () -> context.getInstance(GraphMerger.class);
        graphProvider = () -> context.getInstance(Graph.class);
    }

    @AfterAll
    public void cleanup() {
        vertx.close();
    }

    @Disabled
    @Test
    public void testMergeSimpleGraphs() throws Exception {
        System.out.println("--------- testMergeSimpleGraphs ---------");

        Graph graph1 = loadGraphResource("classpath:/merging/graph.component3.json.gz");
        Graph graph2 = loadGraphResource("classpath:/merging/graph.component4.json.gz");

        JsonObject desc1 = new JsonObject();
        desc1.put(P_FILE_NAME, "graph.component3.json");
        JsonObject desc2 = new JsonObject();
        desc2.put(P_FILE_NAME, "graph.component4.json");
        JsonObject targetDesc = new JsonObject();

        Graph graph = commonMerging(graph1, desc1, graph2, desc2, targetDesc, 0);

        List<JsonObject> apps = graph.traversal().V().hasType(V_APPLICATION).toList();
        assertEquals(0, apps.size());

        //--- Check meta-info
        JsonObject root = graph.getVertex(V_ROOT);
        JsonArray sources = (JsonArray) JsonPointer.from("/assembly/sourceGraphs").queryJson(root);
        JsonArray expSources = new JsonArray()
            .add(new JsonObject().put("type", "component").put("fileName", "graph.component3.json"))
            .add(new JsonObject().put("type", "component").put("fileName", "graph.component4.json"))
            ;
        assertEquals(expSources, sources);

        Object dropped = JsonPointer.from("/assembly/sourcesDropped").queryJson(root);
        JsonArray expDropped = new JsonArray();
        assertEquals(expDropped, dropped);

        // Test patching of domain data from domains.json
        JsonObject domain = graph.getVertex("domain3Id");
        assertNotNull(domain);
        assertEquals("DOMAIN2", domain.getString(F_NAME));
        assertEquals("DEPARTMENT2", domain.getString("department"));
    }

    @Disabled
    @Test
    public void testMergeCompsToApp() throws Exception {
        System.out.println("--------- testMergeCompsToApp ---------");

        Graph graph1 = loadGraphResource("classpath:/merging/graph.component3.json.gz");
        Graph graph2 = loadGraphResource("classpath:/merging/graph.component4.json.gz");

        // Modify source graphs as if those dumps had meta-info
        JsonObject graph1InnerMeta = new JsonObject()
            .put("type", "component")
            .put("name", "component3-dns-name")
            .put("version", "component3Id-version")
            .put("aditVersion", "2.0.0")
            ;
        graph1.getVertex(V_ROOT).put("meta", graph1InnerMeta);
        JsonObject graph2InnerMeta = new JsonObject()
            .put("type", "component")
            .put("name", "component4-dns-name")
            .put("version", "component4Id-version")
            .put("aditVersion", "1.0.0")
            ;
        graph2.getVertex(V_ROOT).put("meta", graph2InnerMeta);

        JsonObject desc1 = new JsonObject();
        desc1.put(P_FILE_NAME, "graph.component3.json");
        JsonObject desc2 = new JsonObject();
        desc2.put(P_FILE_NAME, "graph.component4.json");
        JsonObject targetDesc = new JsonObject();
        targetDesc.put(P_IS_APPLICATION, true);
        targetDesc.put(P_APP_NAME, "AppName");
        targetDesc.put(P_APP_VERSION, "AppVersion");

        Graph graph = commonMerging(graph1, desc1, graph2, desc2, targetDesc, 0);

        List<String> srcFileList = ((JsonArray) GraphMetaInfoSupport.srcGraphsPtr.queryJson(graph.getVertex(V_ROOT)))
            .stream()
            .map(o -> ((JsonObject)o).getString(P_FILE_NAME))
            .collect(Collectors.toList());
        assertEquals(List.of("graph.component3.json", "graph.component4.json"), srcFileList);

        List<JsonObject> apps = graph.traversal().V().hasType(V_APPLICATION).toList();
        assertEquals(1, apps.size());
        JsonObject appVertex = apps.get(0);
        assertEquals("AppName", appVertex.getString(F_NAME));
        assertEquals("AppVersion", appVertex.getString(F_VERSION));

        List<JsonObject> appContents = graph.traversal().V(appVertex.getString(F_ID)).out().toList();
        assertEquals(2, appContents.size());

        appContents.sort(Comparator.comparing(vertex -> vertex.getString(F_ID))) ;

        assertEquals("component3Id", appContents.get(0).getString(F_ID));
        assertEquals("component4Id", appContents.get(1).getString(F_ID));

        //--- Check meta-info
        JsonObject root = graph.getVertex(V_ROOT);

        JsonObject expMeta = new JsonObject()
            .put("type", "application").put("name", "AppName")
            .put("version", "AppVersion").put("aditVersion", GraphMetaInfoSupport.getInventoryToolVersion());
        Object metaInGraph = root.getValue("meta");
        assertEquals(expMeta, metaInGraph);

        JsonArray sources = (JsonArray) JsonPointer.from("/assembly/sourceGraphs").queryJson(root);
        JsonArray expSources = new JsonArray()
            .add(new JsonObject().put("type", "component").put("name", "component3-dns-name")
                .put("fileName", "graph.component3.json").put("version", "component3-version")
                .put("aditVersion", "2.0.0"))
            .add(new JsonObject().put("type", "component").put("name", "component4-dns-name")
                .put("fileName", "graph.component4.json").put("version", "component4-version")
                .put("aditVersion", "1.0.0"))
            ;
        assertEquals(expSources, sources);

        Object dropped = JsonPointer.from("/assembly/sourcesDropped").queryJson(root);
        JsonArray expDropped = new JsonArray();
        assertEquals(expDropped, dropped);
    }

    @Disabled
    @Test
    public void testMergeCompsToAppV2() throws Exception {
        System.out.println("--------- testMergeCompsToApp ---------");

        Graph graph1 = loadGraphResource("classpath:/merging/graph.component3.new.json");
        Graph graph2 = loadGraphResource("classpath:/merging/graph.component7.json");

        JsonObject desc1 = new JsonObject();
        desc1.put(P_FILE_NAME, "graph.component3.new.json");
        JsonObject desc2 = new JsonObject();
        desc2.put(P_FILE_NAME, "graph.component7.json");

        Graph graph = new GraphImpl();
        GraphReport report = new GraphReportImpl();
        graph.setReport(report);
        JsonObject targetDesc = new JsonObject();
        targetDesc.put(P_IS_APPLICATION, true);
        targetDesc.put(P_APP_NAME, "AppName");
        targetDesc.put(P_APP_VERSION, "AppVersion");

        try (MergerApi merger = graphMergerProvider.get()) {
            merger.prepareGraphForMerging(graph, targetDesc);

            merger.mergeGraph(graph1, desc1, graph, targetDesc, false);
            merger.mergeGraph(graph2, desc2, graph, targetDesc, false);

            merger.finalizeGraphAfterMerging(graph, targetDesc);
        }

        List<String> srcFileList = ((JsonArray) GraphMetaInfoSupport.srcGraphsPtr.queryJson(graph.getVertex(V_ROOT)))
                .stream()
                .map(o -> ((JsonObject)o).getString(P_FILE_NAME))
                .collect(Collectors.toList());
        assertEquals(List.of("graph.component3.new.json", "graph.component7.json"), srcFileList);

        List<JsonObject> apps = graph.traversal().V().hasType(V_APPLICATION).toList();
        assertEquals(1, apps.size());
        JsonObject appVertex = apps.get(0);
        assertEquals("AppName", appVertex.getString(F_NAME));
        assertEquals("AppVersion", appVertex.getString(F_VERSION));

        List<JsonObject> appContents = graph.traversal().V(appVertex.getString(F_ID)).out().toList();
        assertEquals(2, appContents.size());

        appContents.sort(Comparator.comparing(vertex -> vertex.getString(F_ID))) ;

        assertEquals("component3Id", appContents.get(0).getString(F_ID));
        assertEquals("component7Id", appContents.get(1).getString(F_ID));

        List<Map<String, JsonObject>> languages = graph.traversal().V("component3Id")
                .outE().as("E")
                .inV().hasType("language").as("L")
                .<JsonObject>select("E","L").toList();
        assertEquals(1, languages.size());
        List<String> languageUsages = JsonUtils.asList(languages.get(0).get("E").getJsonArray("usage"));
        JsonObject languageObject = languages.get(0).get("L");
        List<String> expectedUsages = List.of("source", "target");
        JsonObject expectedLanguage = new JsonObject()
                .put(F_ID, "Java 17")
                .put(F_TYPE, "language")
                .put(F_NAME, "Java")
                .put(F_VERSION, "17");
        assertEquals(expectedUsages, languageUsages);
        assertEquals(expectedLanguage, languageObject);
    }

    @Disabled
    @Test
    public void testMergeOldAppsToNamespace() throws Exception {
        System.out.println("--------- testMergeOldAppsToNamespace ---------");

        Graph graph1 = loadGraphResource("classpath:/merging/graph.component3.old.json");
        Graph graph2 = loadGraphResource("classpath:/merging/graph.component4.old.json");

        JsonObject desc1 = new JsonObject()
            .put(P_IS_APPLICATION, true)
            .put(P_APP_NAME, "AppName1")
            .put(P_APP_VERSION, "AppVersion1");
        JsonObject desc2 = new JsonObject()
            .put(P_IS_APPLICATION, true)
            .put(P_APP_NAME, "AppName2")
            .put(P_APP_VERSION, "AppVersion2");
        JsonObject targetDesc = new JsonObject();
        targetDesc.put(P_IS_NAMESPACE, true);
        targetDesc.put(P_NAMESPACE_NAME, "Namespace");

        Graph graph = commonMerging(graph1, desc1, graph2, desc2, targetDesc, 4);

        // Here we test a modification of source graph
        List<JsonObject> apps = graph.traversal().V().hasType(V_APPLICATION).toList();
        assertEquals(2, apps.size());

        apps.sort(Comparator.comparing(vertex -> vertex.getString(F_NAME))) ;

        JsonObject app1 = apps.get(0);
        assertEquals("AppName1", app1.getString(F_NAME));
        assertEquals("AppVersion1", app1.getString(F_VERSION));
        List<JsonObject> app1Contents = graph.traversal().V(app1.getString(F_ID)).out().toList();
        assertEquals(1, app1Contents.size());
        assertEquals("component3Id", app1Contents.get(0).getString(F_ID));

        JsonObject app2 = apps.get(1);
        assertEquals("AppName2", app2.getString(F_NAME));
        assertEquals("AppVersion2", app2.getString(F_VERSION));
        List<JsonObject> app2Contents = graph.traversal().V(app2.getString(F_ID)).out().toList();
        assertEquals(1, app2Contents.size());
        assertEquals("component4Id", app2Contents.get(0).getString(F_ID));

        //--- Check meta-info
        JsonObject root = graph.getVertex(V_ROOT);

        JsonObject expMeta = new JsonObject()
            .put("type", "namespace")
            .put("name", "Namespace")
            .put("aditVersion", GraphMetaInfoSupport.getInventoryToolVersion());
        Object metaInGraph = root.getValue("meta");
        assertEquals(expMeta, metaInGraph);

        JsonArray sources = (JsonArray) JsonPointer.from("/assembly/sourceGraphs").queryJson(root);
        JsonArray expSources = new JsonArray()
            .add(new JsonObject().put("type", "application").put("name", "AppName1")
                .put("version", "AppVersion1"))
            .add(new JsonObject().put("type", "application").put("name", "AppName2")
                .put("version", "AppVersion2"))
            ;
        assertEquals(expSources, sources);

        Object dropped = JsonPointer.from("/assembly/sourcesDropped").queryJson(root);
        JsonArray expDropped = new JsonArray();
        assertEquals(expDropped, dropped);

        // Test report contents
        JsonObject report1 = new JsonObject()
            .put("type", GraphReport.MERGE_ERROR)
            .put("message", "Source application graph contained no application vertex. Patched.")
            .put("component", "unknown")
            .put("graphId", "APPLICATION:AppName2:AppVersion2");
        assertTrue(graph.getReport().dumpRecords(false).contains(report1), "Missing report on APPLICATION:AppName1:AppVersion1");
        // There must be APPLICATION:AppName1:AppVersion1 as well
    }

    @Disabled
    @Test
    public void testPatchAppVersions() throws Exception {
        System.out.println("--------- testPatchAppVersions ---------");

        Graph graph1 = loadGraphResource("classpath:/merging/graph.component3.old.json");
        Graph graph2 = loadGraphResource("classpath:/merging/graph.component4.old.json");

        // Patch graphs: create application vertices with proper names and links
        new CreateAppVertexTask("AppName1", "unknown").process(graph1);
        new CreateAppVertexTask("AppName2", "another-version").process(graph2);
        // ... and one more application vertex
        new CreateAppVertexTask("AppName3", "AppVersion3").process(graph2);

        JsonObject desc1 = new JsonObject();
        desc1.put(P_IS_APPLICATION, true);
        desc1.put(P_APP_NAME, "AppName1");
        desc1.put(P_APP_VERSION, "AppVersion1");
        JsonObject desc2 = new JsonObject();
        desc2.put(P_IS_APPLICATION, true);
        desc2.put(P_APP_NAME, "AppName2");
        desc2.put(P_APP_VERSION, "AppVersion2");
        JsonObject targetDesc = new JsonObject();
        targetDesc.put(P_IS_NAMESPACE, true);
        targetDesc.put(P_NAMESPACE_NAME, "Namespace");

        Graph graph = commonMerging(graph1, desc1, graph2, desc2, targetDesc, 2);

        // Here we test a modification of source graph
        List<JsonObject> apps = graph.traversal().V().hasType(V_APPLICATION).toList();
        assertEquals(3, apps.size());

        apps.sort(Comparator.comparing(vertex -> vertex.getString(F_NAME))) ;

        JsonObject app1 = apps.get(0);
        assertEquals("AppName1", app1.getString(F_NAME));
        assertEquals("AppVersion1", app1.getString(F_VERSION));
        assertEquals("application:AppName1:AppVersion1", app1.getString(F_ID));
        List<JsonObject> app1Contents = graph.traversal().V(app1.getString(F_ID)).out().toList();
        assertEquals(1, app1Contents.size());
        assertEquals("component3Id", app1Contents.get(0).getString(F_ID));

        JsonObject app2 = apps.get(1);
        assertEquals("AppName2", app2.getString(F_NAME));
        assertEquals("AppVersion2", app2.getString(F_VERSION));
        assertEquals("application:AppName2:AppVersion2", app2.getString(F_ID));
        List<JsonObject> app2Contents = graph.traversal().V(app2.getString(F_ID)).out().toList();
        assertEquals(1, app2Contents.size());
        assertEquals("component4Id", app2Contents.get(0).getString(F_ID));

        JsonObject app3 = apps.get(2);
        assertEquals("AppName3", app3.getString(F_NAME));
        assertEquals("AppVersion3", app3.getString(F_VERSION));
        assertEquals("application:AppName3:AppVersion3", app3.getString(F_ID));
        List<JsonObject> app3Contents = graph.traversal().V(app3.getString(F_ID)).out().toList();
        assertEquals(1, app3Contents.size());
        assertEquals("component4Id", app3Contents.get(0).getString(F_ID));

    }

    @Disabled
    @Test
    public void testRecursiveApp() throws Exception {
        System.out.println("--------- testRecursiveApp ---------");

        Graph graph1 = loadGraphResource("classpath:/merging/graph.component3.json.gz");
        Graph graph2 = loadGraphResource("classpath:/merging/graph.component4.json.gz");

        JsonObject desc1 = new JsonObject();
        desc1.put(P_IS_APPLICATION, true);
        desc1.put(P_APP_NAME, "AppName1");
        desc1.put(P_APP_VERSION, "AppVersion1");
        JsonObject desc2 = new JsonObject();
        desc2.put(P_FILE_NAME, "graph.component4.json");
        JsonObject targetDesc = new JsonObject();
        targetDesc.put(P_IS_APPLICATION, true);
        targetDesc.put(P_APP_NAME, "AppName");
        targetDesc.put(P_APP_VERSION, "AppVersion");

        // Modify graph1 (described as app graph) as if it already had application vertex
        assertEquals(Collections.emptyList(), graph1.traversal().V().hasType(V_APPLICATION).toList());
        new CreateAppVertexTask("AppName1", "AppVersion1").process(graph1);
        assertEquals(Collections.singletonList("AppName1"), graph1.traversal().V().hasType(V_APPLICATION).name().toList());

        Graph graph = commonMerging(graph1, desc1, graph2, desc2, targetDesc, 0);

        List<Map<String, String>> tuples = graph.traversal().V().hasType(V_APPLICATION).name().as("A")
                .out().id().as("C").<String>select("A", "C").toList();
        assertEquals(3, tuples.size());
        Set<Map<String, String>> expTuples = Set.of(
            Map.of("A", "AppName1", "C", "component3"),
            Map.of("A", "AppName", "C", "component3"),
            Map.of("A", "AppName", "C", "component4"));
        assertEquals(expTuples, new HashSet<>(tuples));
    }

    @Disabled
    @Test
    public void testRecreateHttpDependencies() throws Exception {
        System.out.println("--------- testRecreateHttpDependencies ---------");

        Graph graph1 = loadGraphResource("classpath:/merging/graph.component3.old.json");
        Graph graph2 = loadGraphResource("classpath:/merging/graph.component4.old.json");

        JsonObject desc1 = new JsonObject();
        desc1.put(P_IS_APPLICATION, true);
        desc1.put(P_APP_NAME, "AppName1");
        desc1.put(P_APP_VERSION, "AppVersion1");
        JsonObject desc2 = new JsonObject();
        desc2.put(P_IS_APPLICATION, true);
        desc2.put(P_APP_NAME, "AppName2");
        desc2.put(P_APP_VERSION, "AppVersion2");
        JsonObject targetDesc = new JsonObject();

        Graph graph = commonMerging(graph1, desc1, graph2, desc2, targetDesc, 4);

        String[] nonMockDnsNames = { "component3-dns-name", "component4-dns-name" };
        for (String dnsName: nonMockDnsNames) {
            List<JsonObject> mocks = graph.traversal().V().has(P_DETAILS_DNS_NAMES, P.containing(dnsName)).toList();
            assertEquals(1, mocks.size(), "There shall be 1 vertex with dnsName " + dnsName);
            JsonObject mock = mocks.get(0);
            assertEquals(Boolean.FALSE, mock.getValue(F_MOCK_FLAG), "Vertex " + mock.getString(F_ID) + " must NOT be a mock");
        }
        String[] mockDnsNames = { "mock-dns-1", "mock-dns-2", "mock-dns-3", "mock-dns-4",
                "mock-dns-5", "mock-dns-6" };
        for (String dnsName: mockDnsNames) {
            List<JsonObject> mocks = graph.traversal().V().has(P_DETAILS_DNS_NAMES, dnsName).toList();
            assertEquals(1, mocks.size(), "There shall be 1 vertex with dnsName " + dnsName);
            JsonObject mock = mocks.get(0);
            assertEquals(Boolean.TRUE, mock.getValue(F_MOCK_FLAG), "Vertex " + mock.getString(F_ID) + " must be a mock");
        }

        // Test report contents
        JsonArray records = graph.getReport().dumpRecords(false);

        JsonObject reportEntry1 = new JsonObject()
            .put("type", GraphReport.MERGE_ERROR)
            .put("message", "Source application graph contained no application vertex. Patched.")
            .put("component", "unknown")
            .put("graphId", "APPLICATION:AppName1:AppVersion1");
        assertTrue(records.contains(reportEntry1), "Missing report on APPLICATION:AppName1:AppVersion1");
        // There must be APPLICATION:AppName2:AppVersion2 as well

        Set<String> messages = records.stream()
            .map(o -> ((JsonObject)o).getString("message"))
            .collect(Collectors.toSet());
        Set<String> expMessages = Set.of(
            "Reference was not found. Reference: startup http dependency mock-dns-1",
            "Reference was not found. Reference: startup http dependency mock-dns-2",
            "Source application graph contained no application vertex. Patched.");
        assertEquals(expMessages, messages);

        Set<String> expComponents = Set.of("component3Id", "component4Id", "unknown");
        Set<String> reportComponents = records.stream()
            .map(o -> ((JsonObject)o).getString("component"))
            .collect(Collectors.toSet());
        assertEquals(expComponents, reportComponents);

        Set<String> expGraphs = Set.of("APPLICATION:AppName1:AppVersion1", "APPLICATION:AppName2:AppVersion2");
        Set<String> reportGraphs = records.stream()
            .map(o -> ((JsonObject)o).getString("graphId"))
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        assertEquals(expGraphs, reportGraphs);
    }

    @Test
    @Disabled
    public void testMergingWithMultipleDnsNames() throws Exception {
        System.out.println("--------- testMergingWithMultipleDnsNames ---------");

        Graph graph1 = loadGraphResource("classpath:/merging/graph.component3.json");
        Graph graph2 = loadGraphResource("classpath:/merging/graph.component5_modified.json");

        JsonObject targetDesc = new JsonObject();
        JsonObject desc2 = new JsonObject()
                .put(P_FILE_NAME, "graph.component5_modified.json");
        JsonObject desc1 = new JsonObject()
                .put(P_FILE_NAME, "graph.component3.json");
        Graph graph = minimalMerging(graph1, desc1, graph2, desc2, targetDesc);

        String[] nonMockDnsNames = { "component3-dns-name", "component6-dns-name" };
        for (String dnsName: nonMockDnsNames) {
            List<JsonObject> nonMocks = graph.traversal().V().has(P_DETAILS_DNS_NAMES, P.containing(dnsName)).toList();
            assertEquals(1, nonMocks.size(), "There shall be 1 vertex with dnsName " + dnsName);
            JsonObject nonMock = nonMocks.get(0);
            assertEquals(Boolean.FALSE, nonMock.getValue(F_MOCK_FLAG), "Vertex " + nonMock.getString(F_ID) + " must NOT be a mock");
        }
        String[] mockDnsNames = { "mock-dns-1", "mock-dns-2",
                "mock-dns-3", "mock-dns-4" };
        for (String dnsName: mockDnsNames) {
            List<JsonObject> mocks = graph.traversal().V().has(P_DETAILS_DNS_NAME, dnsName).toList();
            assertEquals(1, mocks.size(), "There shall be 1 vertex with dnsName " + dnsName);
            JsonObject mock = mocks.get(0);
            assertEquals(Boolean.TRUE, mock.getValue(F_MOCK_FLAG), "Vertex " + mock.getString(F_ID) + " must be a mock");
        }
        assertEquals(2, graph.traversal().V(V_ROOT).out().hasType(V_DOMAIN)
                .out().has(F_MICROSERVICE_FLAG, P.eq(true)).count().next());
    }

    @Disabled
    @Test
    public void testMergeMockVertices() throws Exception {
        System.out.println("--------- testMergeMockVertices ---------");

        Graph graph1 = loadGraphResource("classpath:/merging/graph.component3Id.json.gz");
        Graph graph2 = loadGraphResource("classpath:/merging/graph.component4Id.json.gz");

        JsonObject desc1 = new JsonObject();
        desc1.put(P_FILE_NAME, "graph.component3Id.json");
        JsonObject desc2 = new JsonObject();
        desc2.put(P_FILE_NAME, "graph.component4Id.json");
        JsonObject targetDesc = new JsonObject();

        //--- Double-check input data

        // component3 graph: non-mock component3, mock component4, 1 edge
        List<JsonObject> components3InComponent3Graph = graph1.traversal().V().has(P_DETAILS_DNS_NAME, "component3-dns-name").toList();
        assertEquals(1, components3InComponent3Graph.size());
        JsonObject component3InComponent3Graph = components3InComponent3Graph.get(0);
        assertEquals("component3Id", component3InComponent3Graph.getString(F_ID));
        assertNotEquals(Boolean.TRUE, component3InComponent3Graph.getValue(F_MOCK_FLAG));

        List<JsonObject> components4IdInComponent3Graph = graph1.traversal().V().has(P_DETAILS_DNS_NAME, "component4-dns-name").toList();
        assertEquals(1, components4IdInComponent3Graph.size());
        JsonObject component4IdInComponent3Graph = components4IdInComponent3Graph.get(0);
        assertEquals(Boolean.TRUE, component4IdInComponent3Graph.getValue(F_MOCK_FLAG));

        List<JsonObject> component3ToComponent4IdEdges1 = graph1.getEdgesBetween(component3InComponent3Graph, component4IdInComponent3Graph);
        assertEquals(1, component3ToComponent4IdEdges1.size());

        // component4Id graph: non-mock component4Id, mock component3, 1 edge
        List<JsonObject> components3InComponent4Graph = graph2.traversal().V().has(P_DETAILS_DNS_NAME, "component3-dns-name").toList();
        assertEquals(1, components3InComponent4Graph.size());
        JsonObject component3InComponent4Graph = components3InComponent4Graph.get(0);
        assertEquals(Boolean.TRUE, component3InComponent4Graph.getValue(F_MOCK_FLAG));

        List<JsonObject> components4InComponent4Graph = graph2.traversal().V().has(P_DETAILS_DNS_NAME, "component4-dns-name").toList();
        assertEquals(1, components4InComponent4Graph.size());
        JsonObject component4IdInComponent4IdGraph = components4InComponent4Graph.get(0);
        assertEquals("component4Id", component4IdInComponent4IdGraph.getString(F_ID));
        assertNotEquals(Boolean.TRUE, component4IdInComponent4IdGraph.getValue(F_MOCK_FLAG));

        List<JsonObject> component4ToComponent3Edges2 = graph2.getEdgesBetween(component4IdInComponent4IdGraph, component3InComponent4Graph);
        assertEquals(1, component4ToComponent3Edges2.size());

        //--- Merge and perform common checks
        Graph graph = commonMerging(graph1, desc1, graph2, desc2, targetDesc, 0);

        //--- Check mock merging

        // Non-mock component3
        List<JsonObject> components3InMergedGraph = graph.traversal().V().has(P_DETAILS_DNS_NAME, "component3-dns-name").toList();
        assertEquals(1, components3InMergedGraph.size());
        JsonObject component3InMergedGraph = components3InMergedGraph.get(0);
        assertEquals("component3Id", component3InMergedGraph.getString(F_ID));
        assertNotEquals(Boolean.TRUE, component3InMergedGraph.getValue(F_MOCK_FLAG));

        // Non-mock component4
        List<JsonObject> component4IdsInMergedGraph = graph.traversal().V().has(P_DETAILS_DNS_NAME, "component4-dns-name").toList();
        assertEquals(1, component4IdsInMergedGraph.size());
        JsonObject component4IdInMergedGraph = component4IdsInMergedGraph.get(0);
        assertEquals("component4Id", component4IdInMergedGraph.getString(F_ID));
        assertNotEquals(Boolean.TRUE, component4IdInMergedGraph.getValue(F_MOCK_FLAG));

        // 1 edge in every direction
        List<JsonObject> component3ToComponent4IdEdges = graph.getEdgesBetween("component3Id", "component4Id");
        assertEquals(1, component3ToComponent4IdEdges.size());

        List<JsonObject> component4ToComponent3Edges = graph.getEdgesBetween("component4Id", "component3Id");
        assertEquals(1, component4ToComponent3Edges.size());
    }

    @Test
    public void testConflictResolution() throws Exception {
        System.out.println("--------- testConflictResolution ---------");
        // Test case: file1 and file3 are two copies of a graph of one component;
        // file2 and file4 are two copies of a graph of another component with the same id assigned

        Graph graph1 = new GraphImpl();
        addComponent(graph1, "domain1", "component1Id", "type1", "https://git1/");
        createFile(graph1, "component1Id", "https://git1/file1");
        JsonObject desc1 = new JsonObject().put(P_FILE_NAME, "graph1.json");

        Graph graph3 = new GraphImpl();
        addComponent(graph3, "domain1", "component1Id", "type1", "https://git1/");
        createFile(graph3, "component1Id", "https://git1/file1");
        JsonObject desc3 = new JsonObject().put(P_FILE_NAME, "graph3.json");

        Graph graph2 = new GraphImpl();
        addComponent(graph2, "domain2", "component1Id", "type2", "https://git2/");
        createFile(graph2, "component1Id", "https://git2/file2");
        JsonObject desc2 = new JsonObject().put(P_FILE_NAME, "graph2.json");

        Graph graph4 = new GraphImpl();
        addComponent(graph4, "domain2", "component1Id", "type2", "https://git2/");
        createFile(graph4, "component1Id", "https://git2/file2");
        JsonObject desc4 = new JsonObject().put(P_FILE_NAME, "graph4.json");

        Graph graph = new GraphImpl();
        GraphReport report = new GraphReportImpl();
        graph.setReport(report);
        JsonObject targetDesc = new JsonObject();

        try (MergerApi merger = graphMergerProvider.get()) {
            merger.prepareGraphForMerging(graph, targetDesc);

            merger.mergeGraph(graph1, desc1, graph, targetDesc, false);
            merger.mergeGraph(graph2, desc2, graph, targetDesc, false);
            merger.mergeGraph(graph3, desc3, graph, targetDesc, false);
            merger.mergeGraph(graph4, desc4, graph, targetDesc, false);

            merger.finalizeGraphAfterMerging(graph, targetDesc);
        }

        // Check components
        List<JsonObject> comps = graph.traversal().V().hasType("type1").toList();
        assertEquals(1, comps.size());
        JsonObject comp1 = comps.get(0);
        String comp1id = comp1.getString(F_ID);
        System.out.println("+++ comp1: " + comp1id);
        assertEquals("https://git1/", comp1.getString(F_REPOSITORY));

        List<JsonObject> altComps = graph.traversal().V("domain1").out().toList();
        assertEquals(1, altComps.size());
        assertSame(comp1, altComps.get(0));

        comps = graph.traversal().V().hasType("type2").toList();
        assertEquals(1, comps.size());
        JsonObject comp2 = comps.get(0);
        String comp2id = comp2.getString(F_ID);
        System.out.println("+++ comp2: " + comp2id);
        assertEquals("https://git2/", comp2.getString(F_REPOSITORY));

        altComps = graph.traversal().V("domain2").out().toList();
        assertEquals(1, altComps.size());
        assertSame(comp2, altComps.get(0));

        // Check files
        List<JsonObject> files = graph.traversal().V(comp1id).out("file").toList();
        assertEquals(1, files.size());
        assertEquals("file", files.get(0).getString(F_TYPE));
        assertEquals("https://git1/file1", files.get(0).getString("fileLink"));

        files = graph.traversal().V(comp2id).out("file").toList();
        assertEquals(1, files.size());
        assertEquals("file", files.get(0).getString(F_TYPE));
        assertEquals("https://git2/file2", files.get(0).getString("fileLink"));

        // Check error reports
        JsonArray reportRecords = graph.getReport().dumpRecords(false);
        int size = reportRecords.size();
        assertTrue(1 <= size && size <= 3, "Wrong size of error report: " + size);
        JsonObject expRecord = new JsonObject()
            .put("type", GraphReport.CONF_ERROR)
            .put("message", "Component duplicated. Source [Id: component1Id, Type: type1, Name: null, Repository: " +
                "https://git1/, Directory: null] Duplicate [Id: component1Id, Type: type2, Name: null, Repository: " +
                "https://git2/, Directory: null]")
            .put("component", "component1Id");
        for (Object record: reportRecords) {
            assertEquals(expRecord, record);
        }
    }

    private void addComponent(Graph graph, String domainId, String id, String type, String repo) {
        JsonObject domain = graph.getVertex(domainId);
        if (domain == null) {
            domain = new JsonObject()
                .put(F_ID, domainId)
                .put(F_TYPE, V_DOMAIN);
            graph.addVertex(domain);
        }
        JsonObject comp = new JsonObject()
            .put(F_ID, id)
            .put(F_TYPE, type)
            .put(F_REPOSITORY, repo);
        graph.addVertex(domain, comp);
    }

    private JsonObject createFile(Graph graph, String comp, String fileLink) {
        JsonObject file = new JsonObject()
            .put(F_ID, UUID.randomUUID().toString())
            .put(F_TYPE, "file")
            .put("fileLink", fileLink);
        JsonObject edge = new JsonObject().put(F_TYPE, "file");
        graph.addEdge(graph.getVertex(comp), file, edge);
        return file;
    }

    @Disabled
    @Test
    public void testAppValidation() throws Exception {
        System.out.println("--------- testAppValidation ---------");

        Graph graph1 = loadGraphResource("classpath:/merging/graph.component1.json.gz");
        Graph graph2 = loadGraphResource("classpath:/merging/graph.component2.json.gz");

        // Patch graph 1: create empty apps
        JsonObject stubApp1 = new JsonObject()
            .put(F_ID, "application:stub1:unknown")
            .put(F_TYPE, V_APPLICATION)
            .put(F_NAME, "stub1");
        graph1.addVertex(stubApp1);
        JsonObject stubApp2 = new JsonObject()
            .put(F_ID, "application:stub2:unknown")
            .put(F_TYPE, V_APPLICATION)
            .put(F_NAME, "stub2");
        graph1.addVertex(stubApp2);

        JsonObject desc1 = new JsonObject()
            .put(P_IS_APPLICATION, true)
            .put(P_APP_NAME, "AppName1")
            .put(P_APP_VERSION, "AppVersion1");
        JsonObject desc2 = new JsonObject()
            .put(P_IS_APPLICATION, true)
            .put(P_APP_NAME, "AppName2")
            .put(P_APP_VERSION, "AppVersion2");
        JsonObject targetDesc = new JsonObject()
            .put(P_IS_NAMESPACE, true)
            .put(P_NAMESPACE_NAME, "Namespace");

        //--- Merge and perform common checks
        Graph graph = commonMerging(graph1, desc1, graph2, desc2, targetDesc, 4);

        // Test applications
        // Note that application "AppName1" is absent!!!
        Map<Object, Object> appAndCounts = graph.traversal().V().hasType(V_APPLICATION)
            .group().by("name").by(__.out().count()).next();
        Map<Object, Object> expAppAndCounts = Map.of(
            "stub1", 0L,
            "stub2", 0L,
            "AppName2", 1L
        );
        assertEquals(expAppAndCounts, appAndCounts);

        // Test report contents
        JsonArray records = graph.getReport().dumpRecords(false);

        JsonObject expRecord1 = new JsonObject()
            .put("type", GraphReport.MERGE_ERROR)
            .put("message", "Application without any component: application:stub1:unknown")
            .put("component", "unknown")
            .put("graphId", "APPLICATION:AppName1:AppVersion1");
        assertTrue(records.contains(expRecord1));

        JsonObject expRecord2 = new JsonObject()
            .put("type", GraphReport.MERGE_ERROR)
            .put("message", "Application without any component: application:stub2:unknown")
            .put("component", "unknown")
            .put("graphId", "APPLICATION:AppName1:AppVersion1");
        assertTrue(records.contains(expRecord2));

        JsonObject expRecord3 = new JsonObject()
            .put("type", GraphReport.MERGE_ERROR)
            .put("message", "Source application graph contained no application vertex. Patched.")
            .put("component", "unknown")
            .put("graphId", "APPLICATION:AppName2:AppVersion2");
        assertTrue(records.contains(expRecord3));

        JsonObject expRecord4 = new JsonObject()
            .put("type", GraphReport.MERGE_ERROR)
            .put("message", "Application name 'AppName1' expected, found: [stub1, stub2]")
            .put("component", "unknown")
            .put("graphId", "APPLICATION:AppName1:AppVersion1");
        assertTrue(records.contains(expRecord4));

    }

    @Disabled
    @Test
    public void testFalloutReport() throws Exception {
        System.out.println("--------- testFalloutReport ---------");

        JsonObject appDesc1 = new JsonObject()
                .put(P_IS_APPLICATION, true)
                .put(P_APP_NAME, "AppName1")
                .put(P_APP_VERSION, "AppVersion1");
        Graph graph1 = mergeForFallout(appDesc1);

        assertNull(graph1.getVertex("null"));
        List<String> components = graph1.traversal().V().hasType("domain").out().id().toList();
        assertEquals(List.of("component1"), components);

        JsonObject resultDump = GraphDumpSupport.dumpToJson(graph1, true);

        List<FalloutDto> fallout = JsonUtils.getFalloutReportFromDump(resultDump);
        assertEquals(1, fallout.size());
        assertEquals(new FalloutDto("AppName1", "AppVersion1", "unknown"), fallout.get(0));

        JsonObject appDesc2 = new JsonObject()
                .put(P_IS_APPLICATION, true)
                .put(P_APP_NAME, "AppName2")
                .put(P_APP_VERSION, "AppVersion2");
        Graph graph2 = mergeForFallout(appDesc2);

        JsonObject namespaceDesc = new JsonObject()
                .put(P_IS_NAMESPACE, true)
                .put(P_NAMESPACE_NAME, "NameSpace");
        Graph namespaceGraph = minimalMerging(graph1, appDesc1, graph2, appDesc2, namespaceDesc);

        graph1 = null;
        graph2 = null;

        assertNull(namespaceGraph.getVertex("null"));
        components = namespaceGraph.traversal().V().hasType("domain").out().id().toList();
        assertEquals(List.of("component1"), components);

        resultDump = GraphDumpSupport.dumpToJson(namespaceGraph, false);
        fallout = JsonUtils.getFalloutReportFromDump(resultDump);
        Set<FalloutDto> cumulativeFallout = Set.of(
            new FalloutDto("AppName1", "AppVersion1", "unknown"),
            new FalloutDto("AppName2", "AppVersion2", "unknown"));
        assertEquals(cumulativeFallout, new HashSet<>(fallout));
    }

    protected Graph mergeForFallout(JsonObject targetDesc) throws Exception {
        Graph graph1 = loadGraphResource("classpath:/merging/graph.component1.json.gz");
        Graph graph2 = loadGraphResource("classpath:/merging/graph.component2.json.gz");

        // Modify source graph component3Id: make it invalid
        graph1.relocateVertex(graph1.getVertex("component1ID"), "null");
        // Modify source graph component4Id as if that dumps had meta-info
        JsonObject graph2InnerMeta = new JsonObject()
            .put("type", "component")
            .put("name", "component2 name")
            .put("version", "component2-version")
            ;
        graph2.getVertex(V_ROOT).put("meta", graph2InnerMeta);

        JsonObject desc1 = new JsonObject();
        desc1.put(P_FILE_NAME, "graph.component1.json");
        JsonObject desc2 = new JsonObject();
        desc2.put(P_FILE_NAME, "graph.component2.json");

        Graph graph = minimalMerging(graph1, desc1, graph2, desc2, targetDesc);
        return graph;
    }

    protected Graph minimalMerging(Graph graph1, JsonObject desc1, Graph graph2, JsonObject desc2, JsonObject targetDesc) {
        Graph graph = new GraphImpl();
        GraphReport report = new GraphReportImpl();
        graph.setReport(report);

        try (MergerApi merger = graphMergerProvider.get()) {
            merger.prepareGraphForMerging(graph, targetDesc);
            merger.mergeGraph(graph1, desc1, graph, targetDesc,false);
            merger.mergeGraph(graph2, desc2, graph, targetDesc,false);
            merger.finalizeGraphAfterMerging(graph, targetDesc);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return graph;
    }

    protected Graph commonMerging(Graph graph1, JsonObject desc1, Graph graph2, JsonObject desc2,
            JsonObject targetDesc, int expectedReportSize) {
        Graph graph = new GraphImpl();
        GraphReport report = new GraphReportImpl();
        graph.setReport(report);

        try (MergerApi merger = graphMergerProvider.get()) {
            merger.prepareGraphForMerging(graph, targetDesc);

            merger.mergeGraph(graph1, desc1, graph, targetDesc, false);
            merger.mergeGraph(graph2, desc2, graph, targetDesc, false);

            merger.finalizeGraphAfterMerging(graph, targetDesc);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        List<JsonObject> domains = graph.traversal().V().hasType("domain").toList();
        assertEquals(1, domains.size());
        assertEquals(Set.of("some_domain"), domains.stream().map(o -> o.getString(F_ID)).collect(Collectors.toSet()));

        List<JsonObject> components = graph.traversal().V().hasType("domain").out().toList();
        assertEquals(2, components.size());
        assertEquals(Set.of("component1", "component2"),
                components.stream().map(obj -> obj.getString(F_ID)).collect(Collectors.toSet()));

        JsonArray reportEntries = report.dumpRecords(false);
        assertEquals(expectedReportSize, reportEntries.size());

        return graph;
    }

    protected Graph loadGraphResource(String location) throws Exception {
        JsonObject dumpFile = JsonUtils.readJsonResource(getClass(), location);
        assertNotNull(dumpFile, "Dump file " + location + " must be non-null");
        Graph graph = graphProvider.get();
        GraphDumpSupport.restoreFromJson(graph, dumpFile);
        return graph;
    }
}
