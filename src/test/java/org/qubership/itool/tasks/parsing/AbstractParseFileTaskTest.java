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

package org.qubership.itool.tasks.parsing;

import org.junit.jupiter.api.Disabled;
import org.qubership.itool.context.FlowContext;
import org.qubership.itool.di.ApplicationContext;
import org.qubership.itool.di.CliModule;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import com.google.inject.Module;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.qubership.itool.modules.graph.Graph.F_DIRECTORY;
import static org.qubership.itool.modules.graph.Graph.F_ID;
import static org.qubership.itool.modules.graph.Graph.F_NAME;

@ExtendWith(VertxExtension.class)
class AbstractParseFileTaskTest {

    private static final JsonObject TEST_DOMAIN = new JsonObject()
            .put(F_ID, "testDomainId");
    private static final JsonObject TEST_COMPONENT = new JsonObject()
            .put(F_ID, "testComponentId")
            .put(F_NAME, "testComponentName")
            .put(F_DIRECTORY, "./target/test-classes/inventory");

    private static final JsonObject TEST_COMPONENT_2 = TEST_COMPONENT.copy()
            .put(F_DIRECTORY, "./target/test-classes/test-folder-with-subfolders");

    private static final JsonObject TEST_COMPONENT_WITH_EXCLUDE_DIRS = TEST_COMPONENT_2.copy()
            .put("excludeDirs", new JsonArray().add("subfolder"));

    static Vertx vertx;
    static JsonObject config;
    static TestParseFileTask testParseFileTask;
    static FlowContext flowContext;

    Checkpoint checkpoint;
    VertxTestContext testContext;

    @BeforeAll
    public static void setUp() {
        vertx = Vertx.vertx();
        config = new JsonObject();
        ApplicationContext appContext = new ApplicationContext(vertx, config, new Module[] {new CliModule(vertx)});
        flowContext = appContext.getInstance(FlowContext.class);
        flowContext.initialize(vertx, config);
    }

    @BeforeEach
    public void initEach() {
        testParseFileTask = new TestParseFileTask();
        flowContext.initialize(testParseFileTask);
        testContext = new VertxTestContext();
        checkpoint = testContext.checkpoint(3);
    }

    @AfterAll
    public static void tearDown() {
        if (vertx != null) {
            vertx.close();
        }
        vertx = null;
    }

    @Test
    void runTestParseTask() throws Throwable {

        testParseFileTask.startInFlow()
                .onComplete(res -> testContext.completeNow());

        assertTrue(testContext.awaitCompletion(5, TimeUnit.SECONDS));
        assertEquals(0, testContext.unsatisfiedCheckpointCallSites().size());

        if (testContext.failed()) {
            throw testContext.causeOfFailure();
        }
    }

    @Test
    void findAllFiles_1() {
        List<String> result = testParseFileTask.findAllFiles(TEST_COMPONENT, List.of(), List.of(), List.of());
        assertEquals( 0, result.size());
    }

    @Test
    void findAllFiles_2() {
        List<String> result = testParseFileTask.findAllFiles(TEST_COMPONENT, List.of(), List.of(Pattern.compile("^.*$")), List.of());
        assertEquals( 3, result.size());
    }

    @Test
    void findAllFiles_3() {
        List<String> result = testParseFileTask.findAllFiles(TEST_COMPONENT, List.of(), List.of(Pattern.compile("^.*$")), List.of());
        assertEquals( 3, result.size());
    }

    @Test
    void findAllFiles_4() {
        List<String> result = testParseFileTask.findAllFiles(TEST_COMPONENT, List.of("t2_inventory.json"), List.of(Pattern.compile("^.*$")), List.of());
        // Are simple patterns supposed to overlap with other kinds of patterns?
        assertEquals( 4, result.size());
    }

    @Disabled("provide proper test folder structure")
    @Test
    void findAllFiles_5() {
        List<String> result = testParseFileTask.findAllFiles(TEST_COMPONENT_2, List.of(), List.of(), List.of("chains/*/*.yaml"));
        assertEquals( 1, result.size());
    }

    @Disabled("provide proper test folder structure")
    @Test
    void findAllFiles_6() {
        List<String> result = testParseFileTask.findAllFiles(TEST_COMPONENT_2, List.of(), List.of(), List.of("**/chain-*.yaml"));
        assertEquals( 1, result.size());
    }

    @Disabled("provide proper test folder structure")
    @Test
    void findAllFiles_7() {
        List<String> result = testParseFileTask.findAllFiles(TEST_COMPONENT_WITH_EXCLUDE_DIRS, List.of(), List.of(), List.of("**/chain-*.yaml"));
        assertEquals( 0, result.size());
    }

    class TestParseFileTask extends AbstractInclusiveParseFileTask {

        @Override
        protected String[] getFilePatterns() {
            return new String[] {
                    "*"
            };
        }

        @Override
        protected void parseSingleFile(JsonObject domain, JsonObject component, String fileName) throws IOException {
            // triggering the checkpoint
            checkpoint.flag();
        }

        @Override
        protected List<Map<String, JsonObject>> getComponentsWithDomains() {
            return List.of(Map.of("C", TEST_COMPONENT, "D", TEST_DOMAIN));
        }
    }
}