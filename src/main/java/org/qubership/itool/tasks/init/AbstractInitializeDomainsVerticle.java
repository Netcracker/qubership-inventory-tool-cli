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

package org.qubership.itool.tasks.init;

import org.qubership.itool.tasks.FlowTask;

import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;

import org.qubership.itool.modules.graph.Graph;
import org.qubership.itool.utils.ConfigUtils;
import org.qubership.itool.utils.FSUtils;
import org.qubership.itool.utils.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.qubership.itool.modules.graph.Graph.F_ID;
import static org.qubership.itool.modules.graph.Graph.F_TYPE;
import static org.qubership.itool.utils.ConfigProperties.INCLUDE_DOMAINS_PROPERTY;
import static org.qubership.itool.utils.ConfigProperties.RELEASE_BRANCH_POINTER;


public abstract class AbstractInitializeDomainsVerticle extends FlowTask {

    protected static final Logger LOG = LoggerFactory.getLogger(AbstractInitializeDomainsVerticle.class);

    private static final String F_COMPONENTS = "components";

    @Override
    protected void taskStart(Promise<?> taskPromise) {
        List<Path> domainConfigs;
        JsonArray includeDomains = getIncludedDomains();

        try (Stream<Path> walk = Files.walk(Paths.get(FSUtils.getConfigFilePath(config(), "config", "domains")))) {
            domainConfigs = walk.filter(Files::isRegularFile)
                .filter(f -> f.toString().endsWith(".json") && containsIn(f, includeDomains))
                .collect(Collectors.toList());
            // TODO: add check if file is readable (filter(Files::isReadableFile)) and send message
            // to report if it is not
            getLogger().debug("Files discovered {}", domainConfigs);
        } catch (IOException e) {
            report.exceptionThrown(new JsonObject(), e);
            domainConfigs = Collections.emptyList();
        }
        for (Path path: domainConfigs) {
            try {
                JsonObject domain = JsonUtils.readJsonFile(path.toString());
                LOG.info("Add Domain " + domain.getString("name") + " (" + domain.getString("id") + ")");
                addDomainToGraph(domain);
            } catch (/* DecodeException | */ IOException e) {
                getLogger().error("Failed to read " + path);
            }
        }

        taskCompleted(taskPromise);
    }

    protected abstract boolean loadComponents();


    protected JsonArray getIncludedDomains () {
        String includeDomains = config().getString(INCLUDE_DOMAINS_PROPERTY);
        JsonArray result = new JsonArray();
        if (includeDomains != null) {
            result = new JsonArray(new ArrayList<>(Arrays.asList(includeDomains.split(",\\s*"))));
        }
        return result;
    }

    protected boolean containsIn(Path f, JsonArray includeDomains) {
        if (includeDomains.isEmpty())
            return true;

        String fileName = f.getFileName().toString();
        return fileName.startsWith("internal_")
            && fileName.endsWith("_domain.json")
            || includeDomains.stream()
                .anyMatch(domain -> fileName.contains(domain.toString() + "_domain.json"));
    }

    private void addDomainToGraph(JsonObject domain) {
        if (Boolean.valueOf(domain.getString("deprecated"))) {
            getLogger().warn("Skipping deprecated domain {}", domain.getString("id"));
            return;
        }

        String domainId = domain.getString(F_ID);
        if ("domain".equals(domain.getString(F_TYPE))) {
            domainId = ConfigUtils.fillDomainId(domainId);
            domain.put(F_ID, domainId);
        }

        Graph graph = this.graph;
        if (!graph.addVertexUnderRoot(domain)) {
            report.componentDuplicated(graph.getVertex(domainId), domain);
        }

        List<JsonObject> components = extractDomainComponents(domain);
        domain.remove(F_COMPONENTS);

        for (JsonObject component: components) {
            Boolean deprecated = Boolean.valueOf(component.getString("deprecated"));
            if (deprecated) {
                getLogger().warn("Skipping deprecated component {}", component.getString("id"));
                continue;
            }
            JsonPointer.from("/details/domain").writeJson(component, domainId, true);
            if (!graph.addVertex(domain, component)) {
                report.componentDuplicated(graph.getVertex(component.getString(F_ID)), component);
            }
            JsonPointer.from("/details/releaseBranch").writeJson(component,
                    decideReleaseBranch(component, domain), true);
        }
    }

    private String decideReleaseBranch(JsonObject c, JsonObject domain) {
        String configValue = ConfigUtils.getConfigValue(RELEASE_BRANCH_POINTER, config());
        String domainOverrideValue = ConfigUtils.getConfigValue("/releaseBranch", domain);
        String componentOverrideValue = ConfigUtils.getConfigValue("/releaseBranch", c);
        if (domainOverrideValue != null) {
            configValue = domainOverrideValue;
        }
        if (componentOverrideValue != null) {
            configValue = componentOverrideValue;
        }
        return configValue;
    }

    protected List<JsonObject> extractDomainComponents(JsonObject domain) {
        if (! loadComponents()) {
            return Collections.emptyList();
        }

        List<JsonObject> components = new ArrayList<>();
        if (domain.getJsonArray(F_COMPONENTS) != null) {
            for (Object c: domain.getJsonArray(F_COMPONENTS)) {
                components.add((JsonObject) c);
            }
        }
        return components;
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

}
