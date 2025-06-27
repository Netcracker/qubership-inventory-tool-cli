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

package org.qubership.itool.cli;

import org.qubership.itool.cli.query.CliQuery;
import org.qubership.itool.tasks.FlowTask;

import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.WorkerExecutor;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import jakarta.inject.Provider;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.qubership.itool.modules.graph.Graph;
import org.qubership.itool.modules.graph.GraphDumpSupport;
import org.qubership.itool.utils.ConfigUtils;
import org.qubership.itool.utils.FSUtils;
import org.qubership.itool.utils.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import static org.qubership.itool.utils.ConfigProperties.QUERY_FILE_POINTER;
import static org.qubership.itool.utils.ConfigProperties.QUERY_STEP_POINTER;
import static org.qubership.itool.utils.ConfigProperties.SUPER_REPOSITORY_DIR_POINTER;
import static org.qubership.itool.utils.ConfigProperties.QUERY_PROGRESS_PATH_POINTER;

public class QueryVerticle extends FlowMainVerticle {
    protected static final Logger LOG = LoggerFactory.getLogger(QueryVerticle.class);

    @Resource
    protected Provider<Graph> graphProvider;

    protected Logger getLogger() {
        return LOG;
    }

    @Override
    public void start() {
        WorkerExecutor executor = vertx.createSharedWorkerExecutor("query-worker-pool"
                , 1
                , 60
                , TimeUnit.DAYS);

        JsonObject config = config();
        String file = ConfigUtils.getConfigValue(QUERY_FILE_POINTER, config);
        String step = ConfigUtils.getConfigValue(QUERY_STEP_POINTER, config);

        if (StringUtils.isNotEmpty(file)) {
            startFromFile(vertx, config, executor, file);
        } else if (StringUtils.isNotEmpty(step)) {
            startFromStep(vertx, config, executor, step);
        } else {
            System.out.println("ERROR: Please specify 'file', or 'step', or 'appId' for CLI");
        }
    }

    private void startFromStep(Vertx vertx, JsonObject config, WorkerExecutor executor, String step) {
        Path progressPath = Path.of(
            ConfigUtils.getConfigValue(SUPER_REPOSITORY_DIR_POINTER, config), //XXX to be removed?
            ConfigUtils.getConfigValue(QUERY_PROGRESS_PATH_POINTER, config),
            FlowTask.TASK_ADDRESS_PREFIX + step + ".json");

        executor.executeBlocking(p -> {
                JsonObject dump = null;
                try {
                    dump = JsonUtils.readJsonFile(progressPath.toString());
                } catch (IOException /* | DecodeException */ e) {
                    p.fail(e);
                }
                if (dump == null) {
                    p.fail("Dump is empty or not found for step " + step);
                }

                Graph graph = graphProvider.get();
                GraphDumpSupport.restoreFromJson(graph, dump);
                if (graph.getVertexCount() == 1) {
                    p.fail("Graph is empty for step " + step);
                    return;
                }
                dump = null;    // Help GC

                System.out.println("Inventory tool Gremlin CLI");
                System.out.println("Graph restored from file: " + progressPath.normalize());
                System.out.println("Total Vertex count: " + graph.getVertexCount());
                System.out.println("For help please enter: help;");

                CliQuery cli = new CliQuery(graph);
                cli.run();
            })
            .onFailure(f -> {
                System.out.println("Gremlin CLI failed to run:\n" + ExceptionUtils.getStackTrace(f));
                vertx.close();
            });
    }

    private void startFromFile(Vertx vertx, JsonObject config, WorkerExecutor executor, String file) {
        Path filePath = Path.of(file);

        executor.executeBlocking(p -> {
                String content = null;
                try {
                    content = FSUtils.readFileAsIs(filePath.toString());
                } catch (IOException e) {
                    p.fail(e);
                }
                if (content == null) {
                    p.fail("Empty data");
                }

                Graph graph = graphProvider.get();
                if (content.startsWith("[")) {
                    loadFromJsonArray(file, graph, p, content);
                } else {
                    GraphDumpSupport.restoreFromJson(graph, new JsonObject(content));
                }
                content = null; // Help GC

                System.out.println("Inventory tool Gremlin CLI");
                System.out.println("Graph restored from file: " + filePath.normalize());
                System.out.println("Total Vertex count: " + graph.getVertexCount());
                System.out.println("Total Edges count: " + graph.getEdgeCount());
                System.out.println("For help please enter: help;");

                CliQuery cli = new CliQuery(graph);
                cli.run();
            })
            .onFailure(f -> {
                System.out.println("Gremlin CLI failed to run:\n" + ExceptionUtils.getStackTrace(f));
                vertx.close();
            });
    }

    private void loadFromJsonArray(String file, Graph graph, Promise<Object> p, String content) {
        JsonArray jsonArray = null;
        try {
            jsonArray = new JsonArray(content);
        } catch (Exception e) {
            p.fail(e);
        }
        if (jsonArray == null) {
            p.fail("JSON file is empty or not found. File: " + file);
        }

        for (Object json : jsonArray) {
            if (!(json instanceof JsonObject)) {
                p.fail("JsonObject expected. Found scalar: " + json);
            }
            JsonObject jsonObj = (JsonObject)json;
            if (jsonObj.getValue(Graph.F_ID) == null) {
                p.fail("JsonObject should contain 'id' property");
            }
            graph.addVertex(jsonObj);
        }
    }

    @Override
    protected List<String> getFlowSequence() {
        return null;    // Not intended to run a flow
    }

}
