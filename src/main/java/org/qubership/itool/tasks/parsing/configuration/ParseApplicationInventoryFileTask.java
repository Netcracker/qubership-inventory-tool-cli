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

import org.qubership.itool.modules.parsing.InventoryJsonParser;
import org.qubership.itool.tasks.FlowTask;

import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;

import org.apache.commons.lang3.StringUtils;
import org.qubership.itool.modules.graph.Graph;
import org.qubership.itool.modules.processor.GraphMetaInfoSupport;
import org.qubership.itool.utils.ConfigUtils;
import org.qubership.itool.utils.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.qubership.itool.cli.ci.CiConstants.*;
import static org.qubership.itool.modules.graph.Graph.F_ID;
import static org.qubership.itool.modules.graph.Graph.F_MOCK_FLAG;
import static org.qubership.itool.modules.graph.Graph.F_REPOSITORY;
import static org.qubership.itool.modules.graph.Graph.F_TYPE;
import static org.qubership.itool.modules.graph.Graph.V_DOMAIN;
import static org.qubership.itool.modules.graph.Graph.V_UNKNOWN;
import static org.qubership.itool.modules.graph.GraphDataConstants.UNKNOWN;
import static org.qubership.itool.modules.graph.GraphDataConstants.UNKNOWN_DOMAIN_NAME;
import static org.qubership.itool.utils.ConfigProperties.RELEASE_BRANCH_POINTER;

import java.io.File;
import java.util.*;

import javax.annotation.Resource;

import jakarta.inject.Provider;

public class ParseApplicationInventoryFileTask extends FlowTask {
    @Resource
    Provider<InventoryJsonParser> inventoryJsonParserProvider;

    protected static final Logger LOG = LoggerFactory.getLogger(ParseApplicationInventoryFileTask.class);

    @Override
    protected void taskStart(Promise<?> taskPromise) throws Exception {
        JsonObject config = config();
        String repository = config.getString(P_REPOSITORY);
        String releaseBranch = (String) JsonPointer.from(RELEASE_BRANCH_POINTER)
                .queryJson(config);
        String inputDir = config.getString(P_INPUT_DIRECTORY);

        File appInvJson = new File(inputDir, "application_inventory.json");
        if (! appInvJson.isFile()) {
            // CiExecCommand ensures that at least one of them exists
            appInvJson = new File(inputDir, "application-inventory.json");
        }
        getLogger().info("Parsing {}", appInvJson);
        InventoryJsonParser parser = inventoryJsonParserProvider.get();

        JsonObject inv = JsonUtils.readJsonFile(appInvJson.getPath());

        String appId = inv.getString("id");
        String appName = inv.getString("name", UNKNOWN);
        String appVersion = inv.getString("version", UNKNOWN);
        if (StringUtils.isEmpty(appId)) {
            appId = String.join(":", "application", appName, appVersion);
        }

        // Fill metainfo in root node
        GraphMetaInfoSupport.initMetaInfoForApplication(graph, appName, appVersion);

        // Create application vertex
        JsonObject application = new JsonObject()
            .put(F_ID, appId)
            .put(F_TYPE, Graph.V_APPLICATION)
            .put(Graph.F_NAME, appName)
            .put(Graph.F_VERSION, appVersion);
        graph.addVertexUnderRoot(application);

        JsonArray components = inv.getJsonArray("components");
        Map<String, JsonObject> domainsCache = new HashMap<>();
        for (Object o: components) {
            JsonObject compDesc = JsonUtils.asJsonObject(o);

            String compId = compDesc.getString("id");
            if (StringUtils.isEmpty(compId)) {
                report.mandatoryValueMissed(new JsonObject().put(F_ID, UNKNOWN), "id");
                continue;
            }
            JsonObject component = new JsonObject()
                .put(F_ID, compId)
                .put(F_TYPE, V_UNKNOWN)
                .put(F_MOCK_FLAG, false)
                .put("releaseBranch", releaseBranch)
                .put(F_REPOSITORY, repository)
                ;

            String domainId = compDesc.getString("domain");
            if (StringUtils.isEmpty(domainId)) {
                report.mandatoryValueMissed(component, "id");
                domainId = UNKNOWN_DOMAIN_NAME;
            } else {
                domainId = ConfigUtils.fillDomainId(domainId);
            }

            // Create domain vertex if needed
            JsonObject domain = domainsCache.computeIfAbsent(domainId, k -> {
                JsonObject mockDomain = new JsonObject()
                    .put(F_ID, k)
                    .put(F_TYPE, V_DOMAIN)
                    .put(F_MOCK_FLAG, true)
                    .put("department", V_UNKNOWN);
                graph.addVertexUnderRoot(mockDomain);
                return mockDomain;
            });

            graph.addVertex(domain, component);
            graph.addEdge(application, component);

            getLogger().info("Component {} created in domain {} and application {}",
                    compId, domainId, appId);

            parser.parse(domain, component, compDesc);
        }

        taskCompleted(taskPromise);
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    protected JsonObject config() { // For tests
        return super.config();
    }

}
