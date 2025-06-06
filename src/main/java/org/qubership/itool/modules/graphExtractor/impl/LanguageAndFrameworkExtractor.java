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

package org.qubership.itool.modules.graphExtractor.impl;

import org.qubership.itool.modules.graph.Graph;
import org.qubership.itool.modules.graphExtractor.AbstractGraphDataExtractor;
import org.qubership.itool.utils.TechNormalizationHelper;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

import static org.qubership.itool.modules.graph.Graph.F_DNS_NAME;
import static org.qubership.itool.modules.graph.Graph.F_ID;
import static org.qubership.itool.modules.graph.Graph.F_NAME;
import static org.qubership.itool.modules.graph.Graph.F_TYPE;
import static org.qubership.itool.modules.graph.Graph.P_DETAILS_DNS_NAME;
import static org.qubership.itool.modules.graph.Graph.V_APPLICATION;
import static org.qubership.itool.modules.graph.Graph.V_ROOT;
import static org.qubership.itool.modules.graph.Graph.V_UNKNOWN;

public class LanguageAndFrameworkExtractor extends AbstractGraphDataExtractor {
    protected static final Logger LOGGER = LoggerFactory.getLogger(LanguageAndFrameworkExtractor.class);

    @Override
    public JsonObject getDataFromGraph(Graph graph) {
        var traversal = graph.traversal();
        JsonObject result = new JsonObject();
        List<Map<String, JsonObject>> componentsWithApps = traversal.V(V_ROOT)
                .out().hasType(V_APPLICATION).as("A")
                .out().as("C")
                .<JsonObject>select("A", "C").toList();
        for (Map<String, JsonObject> componentWithApp : componentsWithApps) {
            JsonObject component = componentWithApp.get("C");
            List<String> languages = traversal.V(component.getString(F_ID))
                    .out().hasType("language")
                    .<String>value("name")
                    .toList();
            List<String> frameworks = traversal.V(component.getString(F_ID))
                    .out().hasType("framework")
                    .<String>value("name")
                    .toList();
            String componentType = component.getString(F_TYPE);
            // Compensation of "component" type from very old graphs
            if ("component".equals(componentType)) {
                componentType = V_UNKNOWN;
            }
            JsonObject componentContent = new JsonObject()
                    .put("languages", new JsonArray(TechNormalizationHelper.getTechsNames(languages)))
                    .put("frameworks", new JsonArray(TechNormalizationHelper.getTechsNames(frameworks)))
                    .put("type", componentType)
                    .put(F_DNS_NAME, JsonPointer.from(P_DETAILS_DNS_NAME).queryJson(component))
                    .put("applicationName", componentWithApp.get("A").getString(F_NAME));
            result.put(component.getString(F_NAME), componentContent);
        }

        return result;
    }

    protected Logger getLogger() {
        return LOGGER;
    }
}
