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

import org.qubership.itool.tasks.FlowTask;

import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;

import org.apache.commons.lang3.StringUtils;
import org.qubership.itool.modules.graph.GraphDataConstants;
import org.qubership.itool.modules.processor.GraphMetaInfoSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.qubership.itool.cli.ci.CiConstants.*;
import static org.qubership.itool.modules.graph.Graph.F_ID;
import static org.qubership.itool.modules.graph.Graph.F_MOCK_FLAG;
import static org.qubership.itool.modules.graph.Graph.F_REPOSITORY;
import static org.qubership.itool.modules.graph.Graph.F_TYPE;
import static org.qubership.itool.modules.graph.Graph.V_DOMAIN;
import static org.qubership.itool.modules.graph.Graph.V_UNKNOWN;
import static org.qubership.itool.utils.ConfigProperties.RELEASE_BRANCH_POINTER;


public class InitializeMockDomainVerticle extends FlowTask {
    protected Logger LOGGER = LoggerFactory.getLogger(InitializeMockDomainVerticle.class);

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

    @Override
    protected void taskStart(Promise<?> taskPromise) {
        JsonObject config = config();

        String directoryPath = config.getString(P_INPUT_DIRECTORY);
        String repository = config.getString(P_REPOSITORY);
        String domainName = config.getString(P_MOCK_DOMAIN);
        String releaseBranch = (String) JsonPointer.from(RELEASE_BRANCH_POINTER)
                .queryJson(config);
        String componentId = findFirst(config.getString(P_COMP_NAME), config.getString(P_RUN_NAME), GraphDataConstants.UNKNOWN);
        String componentName = findFirst(config.getString(P_COMP_NAME), GraphDataConstants.UNKNOWN);
        String componentVersion = findFirst(config.getString(P_COMP_VERSION), GraphDataConstants.UNKNOWN);

        JsonObject mockDomain = new JsonObject()
                .put(F_ID, domainName)
                .put(F_TYPE, V_DOMAIN)
                .put("department", V_UNKNOWN)
                .put(F_MOCK_FLAG, true);
        graph.addVertexUnderRoot(mockDomain);

        JsonObject component = new JsonObject()
                .put(F_ID, componentId)
                .put(F_MOCK_FLAG, true)
                .put(F_TYPE, V_UNKNOWN)
                .put("directoryPath", directoryPath)
                .put("releaseBranch", releaseBranch)
                .put(F_REPOSITORY, repository)
                ;
        getLogger().info("Seed component created: {}", component);

        JsonPointer.from("/details/domain").writeJson(component, domainName, true);
        JsonPointer.from("/details/releaseBranch").writeJson(component, releaseBranch, true);

        graph.addVertex(mockDomain, component);

        // Fill metainfo in root node
        GraphMetaInfoSupport.initMetaInfoForComponent(graph, componentName, componentVersion);

        taskCompleted(taskPromise);
    }

    private static String findFirst(String... strings) {
        for (String s: strings) {
            if (StringUtils.isNotBlank(s)) {
                return s;
            }
        }
        return GraphDataConstants.UNKNOWN;
    }

}
