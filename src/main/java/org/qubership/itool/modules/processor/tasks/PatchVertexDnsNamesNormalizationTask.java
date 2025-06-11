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
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;

import static org.qubership.itool.modules.graph.Graph.F_DETAILS;
import static org.qubership.itool.modules.graph.Graph.F_DNS_NAME;
import static org.qubership.itool.modules.graph.Graph.F_DNS_NAMES;
import static org.qubership.itool.modules.graph.Graph.F_MOCKED_FOR;
import static org.qubership.itool.modules.graph.Graph.F_MOCK_FLAG;
import static org.qubership.itool.modules.graph.Graph.P_DETAILS_DNS_NAME;
import static org.qubership.itool.modules.graph.Graph.P_DETAILS_DNS_NAMES;

/**
 * <p>Patch DNS names on vertices of old graphs with from versions 1 and less.
 * Intended to be applied to source graphs before merging.
 *
 * <p>Actions:
 * <ul><li>If component vertex doesn't have "/details/dnsNames" property, create it
 * </ul>
 */
public class PatchVertexDnsNamesNormalizationTask implements GraphProcessorTask {

    private static final Logger LOG = LoggerFactory.getLogger(PatchVertexDnsNamesNormalizationTask.class);

    @Override
    public void process(Graph graph) throws InvalidGraphException {
        // In version 2 we got multiple DNS names in components
        if (graph.getGraphVersion() >= 2) {
            LOG.debug("Skipping task {}, because graph with version {} should support multiple DNS names", getClass().getSimpleName(), graph.getGraphVersion());
            return;
        }

        long startTime = System.nanoTime();
        LOG.debug("Starting task {}", getClass().getSimpleName());

        List<JsonObject> existingComponents = graph.traversal().V().hasKey(P_DETAILS_DNS_NAME).toList();
        for (JsonObject component: existingComponents) {
            // All components there have "/details/dnsNames" in them by the criteria in query, so no checks for null are required while accessing either of those values

            // Update mock vertex
            if (Boolean.TRUE.equals(component.getValue(F_MOCK_FLAG))) {
                JsonArray mockedFor = component.getJsonArray(F_MOCKED_FOR);
                if (!mockedFor.contains(P_DETAILS_DNS_NAME)) {
                    continue;
                }
                mockedFor.remove(P_DETAILS_DNS_NAME);
                mockedFor.add(P_DETAILS_DNS_NAMES);
                JsonObject details = component.getJsonObject(F_DETAILS);
                String dnsName = details.getString(F_DNS_NAME);
                details.put(F_DNS_NAMES, dnsName);
            }

            // Update normal vertex
            JsonObject details = component.getJsonObject(F_DETAILS);
            if (!details.containsKey(F_DNS_NAMES)) {
                details.put(F_DNS_NAMES, new JsonArray().add(details.getString(F_DNS_NAME)));
            }
        }
        long endTime = System.nanoTime();
        LOG.debug("Task completed in {} ", Duration.ofNanos(endTime - startTime));
    }

}
