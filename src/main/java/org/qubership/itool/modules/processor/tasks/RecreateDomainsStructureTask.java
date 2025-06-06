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
import org.qubership.itool.modules.graph.GraphDataConstants;
import org.qubership.itool.modules.processor.InvalidGraphException;
import org.qubership.itool.modules.report.GraphReport;
import org.qubership.itool.modules.report.GraphReportImpl;
import org.qubership.itool.utils.JsonUtils;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.qubership.itool.modules.graph.Graph.F_ID;

public class RecreateDomainsStructureTask implements GraphProcessorTask {

    static final Logger LOG = LoggerFactory.getLogger(RecreateDomainsStructureTask.class);

    private static final String DOMAIN_RESOURCE_PATH = "classpath:/domains.json";

    GraphReport graphReport = new GraphReportImpl();

    public void process(Graph graph) throws InvalidGraphException {
        Map<String, JsonObject> domainsDataFromReference = readJsonResource(DOMAIN_RESOURCE_PATH);
        List<JsonObject> domainsFromGraph = graph.traversal().V().hasType("domain").toList();

        for (JsonObject domainFromGraph : domainsFromGraph) {
            String domainId = domainFromGraph.getString("id");
            JsonObject domainData = domainsDataFromReference.get(domainId);
            if (GraphDataConstants.UNKNOWN_DOMAIN_NAME.equals(domainId)) {
                // Pseudo-domain that contains components without inventory file
                List<JsonObject> components = graph.traversal().V(domainId).out().toList();
                for (JsonObject sourceComponent : components) {
                    LOG.warn("Component '{}' belongs to pseudo-domain '{}'", sourceComponent.getString(F_ID), domainId);
                }
            } else if (domainData != null) {
                domainFromGraph.put("technical-manager", domainData.getString("technical-manager"));
                domainFromGraph.put("department", domainData.getString("department"));
                domainFromGraph.put(Graph.F_NAME, domainData.getString(Graph.F_NAME));
                domainFromGraph.put(Graph.F_MOCK_FLAG, false);
            } else {
                //Ignore unknown domains without components
                List<JsonObject> components = graph.traversal().V(domainId).out().toList();
                for (JsonObject sourceComponent : components) {
                    // Report component belonging to unidentified domain
                    graphReport.referenceNotFound(sourceComponent, domainId);
                }
            }
        }
    }

    protected Map<String, JsonObject> readJsonResource(String resourcePath) {
        Map<String, JsonObject> domainDataMap = new HashMap<>();
        try {
            JsonArray domains = JsonUtils.readJsonResource(getClass(), resourcePath, JsonArray.class);
            for (Object domain : domains) {
                JsonObject element = (JsonObject) domain;
                String domainId = element.getString(F_ID);
                domainId = domainId.startsWith("D_") ? domainId : "D_" + domainId;
                element.put(F_ID, domainId);
                domainDataMap.put(domainId, element);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to read JSON resource file: " + e.getMessage(), e);
        }
        return domainDataMap;
    }
}
