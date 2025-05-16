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

package org.qubership.itool.tasks.ci;

import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;

import org.qubership.itool.modules.graph.Graph;
import org.qubership.itool.modules.graph.GraphDataConstants;
import org.qubership.itool.modules.processor.GraphMerger;
import org.qubership.itool.modules.processor.MergerApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.qubership.itool.cli.ci.CiConstants;
import org.qubership.itool.tasks.FlowTask;
import org.qubership.itool.modules.processor.GraphMergerFactory;

import java.nio.file.Path;

import javax.annotation.Resource;

public class MergeGraphsVerticle extends FlowTask {
    protected static Logger LOGGER = LoggerFactory.getLogger(MergeGraphsVerticle.class);

    @Resource
    private GraphMergerFactory graphMergerFactory;

    @Override
    protected String[] features() {
        return new String[]{"unskippable"};
    }

    @Override
    protected void taskStart(Promise<?> taskPromise) {
        vertx.executeBlocking(promise -> {
            try (GraphMerger merger = graphMergerFactory.createGraphMerger(vertx, false)) {
                JsonObject targetDesc = new JsonObject()
                    .put(MergerApi.P_IS_APPLICATION, true)
                    .put(MergerApi.P_APP_NAME, config().getString(CiConstants.P_APP_NAME, GraphDataConstants.UNKNOWN))
                    .put(MergerApi.P_APP_VERSION, config().getString(CiConstants.P_APP_VERSION, GraphDataConstants.UNKNOWN));

                merger.prepareGraphForMerging(graph, targetDesc);
                merger.walkAndMerge(
                    Path.of(config().getString(CiConstants.P_INPUT_DIRECTORY)),
                    this.graph,
                    targetDesc);
                merger.finalizeGraphAfterMerging(graph, targetDesc);

            } catch (Exception e) {
                report.exceptionThrown(new JsonObject().put(Graph.F_ID, "inventory-tool"), e);
//                promise.tryFail(e);  // XXX Shall we fail the entire flow?
            } finally {
                promise.tryComplete();
            }
        }, res -> {
            taskCompleted(taskPromise);
        });
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

}
