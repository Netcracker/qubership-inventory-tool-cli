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

import java.util.List;

import static org.qubership.itool.modules.graph.Graph.F_MOCK_FLAG;
import static org.qubership.itool.modules.graph.Graph.F_NAME;
import static org.qubership.itool.modules.graph.Graph.F_REPOSITORY;
import static org.qubership.itool.modules.graph.Graph.V_APPLICATION;
import static org.qubership.itool.modules.graph.Graph.V_DOMAIN;
import static org.qubership.itool.modules.graph.Graph.V_ROOT;
import static org.qubership.itool.modules.gremlin2.P.eq;
import static org.qubership.itool.modules.gremlin2.P.within;

public class PatchMockedComponentsNormalizationTask implements GraphProcessorTask {

    private static final Logger LOG = LoggerFactory.getLogger(PatchMockedComponentsNormalizationTask.class);

    @Override
    public void process(Graph graph) throws InvalidGraphException {
        List<JsonObject> mockComponents = graph.traversal().V(V_ROOT)
                .out().hasType(V_APPLICATION, V_DOMAIN)
                .out().has(F_MOCK_FLAG, eq(true))
                .dedup()
                .toList();
        if (LOG.isDebugEnabled() && mockComponents.size()>0) {
            LOG.debug("Found {} mock components, checking if any of them are real components", mockComponents.size());
        }
        for (JsonObject component : mockComponents) {
            if (isNotMockComponent(component)) {
                component.put(F_MOCK_FLAG, false);
                LOG.info("Corrected the mock flag for component with name {}", component.getString(F_NAME));
            }
        }

    }

    private boolean isNotMockComponent(JsonObject component) {
        return component.getString(F_REPOSITORY) != null
                && ! "null".equalsIgnoreCase(component.getString(F_REPOSITORY));
    }
}
