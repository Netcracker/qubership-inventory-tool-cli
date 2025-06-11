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

package org.qubership.itool.modules.processor.tasks;

import java.time.Duration;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.qubership.itool.modules.graph.Graph;
import org.qubership.itool.modules.graph.GraphDataConstants;
import org.qubership.itool.modules.processor.InvalidGraphException;

import io.vertx.core.json.JsonObject;

import static org.qubership.itool.modules.processor.MergerApi.*;

/**
 * <p>Create Application vertex. May be applied to source graphs before merging
 * and to resulting graph after merging.
 *
 * <p>Pre-condition: This task is applied to Application graph (otherwise, it does nothing)
 *
 * <p>Actions: New Application vertex is created and linked to all the components.
 * If there exist other application vertices inside the graph, they are retained.
 */
public class CreateAppVertexTask implements GraphProcessorTask {

    private static final Logger LOG = LoggerFactory.getLogger(CreateAppVertexTask.class);

    protected final String appName;
    protected final String appVersion;
    protected boolean disabled;

    public CreateAppVertexTask(String appName, String appVersion) {
        if (StringUtils.isEmpty(appVersion)) {
            appVersion = GraphDataConstants.UNKNOWN;
        }
        this.appName = appName;
        this.appVersion = appVersion;
    }

    public CreateAppVertexTask(JsonObject desc) {
        this(desc.getString(P_APP_NAME), desc.getString(P_APP_VERSION));

        if (! desc.getBoolean(P_IS_APPLICATION, false)) {
            LOG.debug("Target is not an application: {}", desc);
            disabled = true;
        }
    }

    @Override
    public void process(Graph graph) throws InvalidGraphException {
        if (disabled) {
            LOG.info("Skipping task");
            return;
        }

        long startTime = System.nanoTime();
        LOG.info("Starting task");

        List<JsonObject> existingApps = graph.traversal().V().hasType(Graph.V_APPLICATION).toList();
        for (JsonObject existingApp: existingApps) {
            LOG.warn("Source graph already contain app vertex {} when trying to assemble an app graph: {}:{}",
                    existingApp.getValue(Graph.F_ID), appName, appVersion);
        }

        String appVertexId = String.join(":", "application", appName, appVersion);
        LOG.info("Creating app vertex: {}", appVertexId);
        JsonObject applicationVertex = new JsonObject()
            .put(Graph.F_ID, appVertexId)
            .put(Graph.F_NAME, appName)
            .put(Graph.F_TYPE, Graph.V_APPLICATION)
            .put(Graph.F_VERSION, appVersion);
        graph.addEdge(graph.getVertex(Graph.V_ROOT), applicationVertex);

        List<JsonObject> outGoingVertices = graph.traversal().V().hasType(Graph.V_DOMAIN).out().toList();
        for (JsonObject outGoingVertex : outGoingVertices) {
            LOG.debug("Linking app {} to component {}", appVertexId, outGoingVertex.getString(Graph.F_ID));
            graph.addEdge(applicationVertex, outGoingVertex);
        }

        long endTime = System.nanoTime();
        LOG.info("Task completed in {}", Duration.ofNanos(endTime - startTime));
    }

}
