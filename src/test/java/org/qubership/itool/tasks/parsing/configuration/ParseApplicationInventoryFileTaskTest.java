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

package org.qubership.itool.tasks.parsing.configuration;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.qubership.itool.modules.graph.Graph;
import org.qubership.itool.modules.gremlin2.P;
import org.qubership.itool.modules.processor.GraphMetaInfoSupport;
import org.qubership.itool.utils.FutureUtils;
import org.qubership.itool.utils.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.qubership.itool.cli.ci.CiConstants;
import org.qubership.itool.context.FlowContext;
import org.qubership.itool.di.ApplicationContext;
import org.qubership.itool.di.CliModule;
import org.qubership.itool.tasks.dependency.SetEdgesBetweenComponentsVerticle;
import org.qubership.itool.tasks.init.InitializeDomainsVerticle;
import com.google.inject.Module;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.jackson.DatabindCodec;
import io.vertx.core.json.pointer.JsonPointer;
import io.vertx.junit5.VertxExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.qubership.itool.modules.graph.Graph.P_DETAILS_DNS_NAME;


@ExtendWith(VertxExtension.class)
public class ParseApplicationInventoryFileTaskTest {

    protected static final Logger LOG = LoggerFactory.getLogger(ParseApplicationInventoryFileTaskTest.class);

    private static final String REPO_ADDR = "https://git.host.name/test.git";
    private static final long TIMEOUT = 60;
    private static final TimeUnit TIMEUNIT = TimeUnit.SECONDS;

    JsonObject config;
    Graph graph;
    InitializeDomainsVerticle task0;
    ParseApplicationInventoryFileTask task1;
    SetEdgesBetweenComponentsVerticle task2;
    Vertx vertx;

    @BeforeEach
    public void setUp() throws Exception {
        // Force class initialization here
        DatabindCodec.mapper();

        config = new JsonObject();
        config.put(CiConstants.P_INPUT_DIRECTORY, "./target/test-classes/app-inventory");
        config.put(CiConstants.P_REPOSITORY, REPO_ADDR);

        vertx = Vertx.vertx();
        ApplicationContext appContext = new ApplicationContext(vertx, config, new Module[] {new CliModule(vertx)});
        FlowContext flowContext = appContext.getInstance(FlowContext.class);
        flowContext.initialize(vertx, config);
        graph = flowContext.getGraph();

        task0 = new InitializeDomainsVerticle();
        flowContext.initialize(task0);
        task1 = new ParseApplicationInventoryFileTask();
        flowContext.initialize(task1);
        task2 = new SetEdgesBetweenComponentsVerticle();
        flowContext.initialize(task2);
    }

    @AfterEach
    public void tearDown() {
        if (vertx != null) {
            vertx.close();
        }
        vertx = null;
    }

    @Test
    public void testParser() throws Exception {

        String location = task1.config().getString(CiConstants.P_INPUT_DIRECTORY);
        LOG.info("Contents of {} : {}", location, new File(location).listFiles());

        // Emulate partial flow
        JsonObject infraDomain = JsonUtils.readJsonFile("./inventory-tool/default/config/domains/internal_infra_domain.json");
        //task0.addDomainToGraph(infraDomain);
        MethodUtils.invokeMethod(task0, true, "addDomainToGraph", infraDomain);
        Future<?> flow = task1.startInFlow()
            .compose(r -> task2.startInFlow())
            ;

        // Wait for the flow to finish
        AsyncResult<?> res = FutureUtils.blockForAsyncResult(flow, TIMEOUT, TIMEUNIT);
        assertNotNull(res);
        assertTrue(res.succeeded());

        // Check graph meta-info
        JsonObject meta = graph.getVertex(Graph.V_ROOT).getJsonObject("meta");
        JsonObject expMeta = new JsonObject()
            .put("type", "application")
            .put("name", "app-name")
            .put("version", "app-version")
            .put("aditVersion", GraphMetaInfoSupport.getInventoryToolVersion());
        assertEquals(expMeta, meta);

        // Check graph contents
        Set<String> domains = graph.traversal().V().hasType(Graph.V_DOMAIN).id().toSet();
        Set<String> expDomains = Set.of("D_domain-name-2", "D_domain_name_1");
        assertEquals(expDomains, domains);

        List<JsonObject> apps = graph.traversal().V().hasType(Graph.V_APPLICATION).toList();
        assertEquals(1, apps.size());
        JsonObject app = apps.get(0);
        assertEquals("app-name", app.getString(Graph.F_NAME));
        assertEquals("app-version", app.getString(Graph.F_VERSION));

        Set<String> comps = graph.traversal().V().hasType(Graph.V_APPLICATION).out().id().toSet();
        Set<String> expComps = Set.of("component_id_1", "component_id_2");
        assertEquals(expComps, comps);

        // Check just attributes of components set by ParseApplicationInventoryFileTask
        JsonObject comp1 = graph.getVertex("component_id_1");
        checkComp1(comp1);
        JsonObject comp2 = graph.getVertex("component_id_2");
        checkComp2(comp2);

        // Check links set by SetEdgesBetweenComponentsVerticle
        Set<Map<Object, Object>> httpLinks1 = graph.traversal().V("component_id_1")
            .outE("mandatory", "optional", "startup").as("E")
            .inV().as("T").has("type", P.neq("database"))
            .select("E", "T").values("ET:/E/type", "TD:/T/details/dnsName").toSet();
        Set<Map<Object, Object>> expLinks1 = Set.of(
            Map.of("ET", "startup", "TD", "component-3-dns-name"),
            Map.of("ET", "mandatory", "TD", "component-4-dns-name"),
            Map.of("ET", "optional", "TD", "component-5-dns-name"),
            Map.of("ET", "optional", "TD", "component-6-dns-name"));
        assertEquals(expLinks1, httpLinks1);

        Set<Map<Object, Object>> httpLinks2 = graph.traversal().V("component_id_2")
            .outE("mandatory", "optional", "startup").as("E")
            .inV().as("T").has("type", P.neq("database"))
            .select("E", "T").values("ET:/E/type", "TD:/T/details/dnsName").toSet();
        Set<Map<Object, Object>> expLinks2 = Set.of(
            Map.of("ET", "startup", "TD", "component-3-dns-name"),
            Map.of("ET", "mandatory", "TD", "component-5-dns-name"),
            Map.of("ET", "optional", "TD", "component-6-dns-name"),
            Map.of("ET", "optional", "TD", "component-7-dns-name"));
        assertEquals(expLinks2, httpLinks2);

        List<String> dbLinks1 = graph.traversal().V("component_id_1")
            .out("mandatory").as("T").has("type", "database").id().toList();
        List<String> expDb1 = List.of("PostgreSQL");
        assertEquals(expDb1, dbLinks1);

        List<String> dbLinks2 = graph.traversal().V("component_id_2")
            .out("mandatory").as("T").has("type", "database").id().toList();
        List<String> expDb2 = List.of();
        assertEquals(expDb2, dbLinks2);

        Set<Map<Object, Object>> producers1 = graph.traversal().V("component_id_1")
            .outE("producer").as("E").inV().as("T")
            .select("E", "T").values("QN:/E/name", "QP:/T/id").toSet();
        Set<Map<Object, Object>> expProducers1 = Set.of(
            Map.of("QN", "topic1", "QP", "Kafka"));
        assertEquals(expProducers1, producers1);

        Set<Map<Object, Object>> consumers1 = graph.traversal().V("component_id_1")
            .inE("consumer").as("E").outV().as("T")
            .select("E", "T").values("QN:/E/name", "QP:/T/id").toSet();
        Set<Map<Object, Object>> expConsumers1 = Set.of(
            Map.of("QN", "topic2", "QP", "RabbitMQ"));
        assertEquals(expConsumers1, consumers1);

    }

    @Test
    public void testFallback() throws Exception {
        String oldLocation = config.getString(CiConstants.P_INPUT_DIRECTORY);
        String newLocation = oldLocation + "_1";
        config.put(CiConstants.P_INPUT_DIRECTORY, newLocation);

        File newDir = new File(newLocation);
        LOG.info("Creating directory {}", newDir);
        newDir.mkdirs();

        File oldFile = new File(oldLocation, "application_inventory.json");
        File newFile = new File(newDir, "application-inventory.json");
        LOG.info("Copying {} -> {}", oldFile, newFile);
        FileUtils.copyFile(oldFile, newFile);

        testParser();

        LOG.info("Removing directory {}", newDir);
        FileUtils.deleteDirectory(newDir);
    }

    private void checkComp1(JsonObject comp1) {
        assertEquals("component-1-dns-name", JsonPointer.from(P_DETAILS_DNS_NAME).queryJson(comp1));
        assertEquals("D_domain_name_1", JsonPointer.from("/details/domain").queryJson(comp1));
        assertEquals(REPO_ADDR, comp1.getString(Graph.F_REPOSITORY));
        assertNull(comp1.getString("repositorySubDir"));

        JsonArray expDatabase = new JsonArray()
            .add(new JsonObject().put("item", "PostgreSQL 15").put("viaZookeeper", "no"));
        assertEquals(expDatabase, JsonPointer.from("/details/database/database").queryJson(comp1));

        JsonObject expMQs = new JsonObject(
            "{ \"kafka\" : {"
            + "  \"producer\" : [ \"topic1\" ]"
            + " },"
            + " \"rabbitMQ\" : {"
            + "  \"consumer\" : [ \"topic2\" ]"
            + " }"
            + "}");
        assertEquals(expMQs, JsonPointer.from("/details/messageQueues").queryJson(comp1));

        JsonObject expDeps = new JsonObject(
            "{ \"startup\" : [ \"component-3-dns-name\" ],"
            + " \"mandatory\" : [ \"component-4-dns-name\" ],"
            + " \"optional\" : [ \"component-5-dns-name\", \"component-6-dns-name\" ]"
            + "}");
        assertEquals(expDeps, JsonPointer.from("/details/dependencies").queryJson(comp1));
    }

    private void checkComp2(JsonObject comp2) {
        assertEquals("component-2-dns-name", JsonPointer.from(P_DETAILS_DNS_NAME).queryJson(comp2));
        assertEquals("D_domain-name-2", JsonPointer.from("/details/domain").queryJson(comp2));
        assertEquals(REPO_ADDR, comp2.getString(Graph.F_REPOSITORY));
        assertNull(comp2.getString("repositorySubDir"));

        JsonArray expDatabase = new JsonArray();
        assertEquals(expDatabase, JsonPointer.from("/details/database/database").queryJson(comp2));

        JsonObject expMQs = null;
        assertEquals(expMQs, JsonPointer.from("/details/messageQueues").queryJson(comp2));

        JsonObject expDeps = new JsonObject(
            "{ \"startup\" : [ \"component-3-dns-name\" ],"
            + " \"mandatory\" : [ \"component-5-dns-name\" ],"
            + " \"optional\" : [ \"component-6-dns-name\", \"component-7-dns-name\" ]"
            + "}");
        assertEquals(expDeps, JsonPointer.from("/details/dependencies").queryJson(comp2));
    }

}
