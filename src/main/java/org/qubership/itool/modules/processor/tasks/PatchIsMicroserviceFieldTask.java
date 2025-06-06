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

import org.qubership.itool.modules.graph.Graph;
import org.qubership.itool.modules.processor.InvalidGraphException;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;

import static org.qubership.itool.modules.graph.Graph.F_MICROSERVICE_FLAG;
import static org.qubership.itool.modules.graph.Graph.V_DOMAIN;
import static org.qubership.itool.modules.graph.Graph.V_ROOT;
import static org.qubership.itool.utils.GraphHelper.isComponentAMicroservice;

public class PatchIsMicroserviceFieldTask implements GraphProcessorTask {

    private static final Logger LOG = LoggerFactory.getLogger(PatchIsMicroserviceFieldTask.class);

    @Override
    public void process(Graph graph) throws InvalidGraphException {
        // In version 3 the new flag was introduced
        if (graph.getGraphVersion() >= 3) {
            LOG.debug("Skipping task {}, because graph with version {} should have correct isMicroservice fields in components",
                    getClass().getSimpleName(), graph.getGraphVersion());
            return;
        }

        long startTime = System.nanoTime();
        LOG.debug("Starting task {}", getClass().getSimpleName());

        List<JsonObject> existingComponents = graph.traversal().V(V_ROOT)
                .out().hasType(V_DOMAIN)
                .out()
                .toList();
        for (JsonObject component: existingComponents) {
            if (component.getBoolean(F_MICROSERVICE_FLAG) == null) {
                component.put(F_MICROSERVICE_FLAG, isComponentAMicroservice(graph, component));
            }
        }

        long endTime = System.nanoTime();
        LOG.debug("Task completed in {} ", Duration.ofNanos(endTime - startTime));
    }

}
