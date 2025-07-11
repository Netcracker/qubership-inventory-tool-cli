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

package org.qubership.itool.tasks.confluence;

import freemarker.template.TemplateMethodModelEx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.openapi4j.core.exception.ResolutionException;
import org.openapi4j.core.validation.ValidationException;
import org.openapi4j.parser.OpenApi3Parser;
import org.openapi4j.parser.model.v3.OpenApi3;
import org.qubership.itool.modules.diagram.DiagramService;
import org.qubership.itool.modules.gremlin2.graph.GraphTraversal;
import org.qubership.itool.modules.template.ConfluencePage;
import org.qubership.itool.modules.template.DiagramMicroserviceMethod;
import org.qubership.itool.utils.LanguageUtils;
import org.qubership.itool.utils.TechNormalizationHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Provider;

import javax.annotation.Resource;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.qubership.itool.modules.gremlin2.P.*;
import static org.qubership.itool.modules.gremlin2.graph.__.*;
import static org.qubership.itool.modules.gremlin2.structure.MapElement.both;
import static org.qubership.itool.modules.gremlin2.structure.MapElement.key;

public class ConfluenceGenerateComponentPagesVerticle extends AbstractConfluenceGenerationPageVerticle {
    @Resource
    protected DiagramService diagramService;
    @Resource
    protected Provider<ConfluencePage> confluencePageProvider;

    public static final List<String> BACKEND_TYPES = Arrays.asList("backend", "ui backend");

    protected Logger LOG = LoggerFactory.getLogger(ConfluenceGenerateComponentPagesVerticle.class);


    @Override
    protected List<ConfluencePage> prepareConfluencePageList(String department) {
        List<ConfluencePage> confluencePageList = new ArrayList<>();

        List<JsonObject> components = V().hasType("domain").has("department", department).out()
                .hasKeys("/details/domain")
                .toList();

        // TODO: collect pages from different components in multiple threads. See AbstractAggregationTaskVerticle
        for (JsonObject component : components) {
            String domain = (String) JsonPointer.from("/details/domain").queryJson(component);
            String componentId = component.getString("id");
            getLogger().info("Processing: {}", componentId);
            if (null == domain) {
                report.mandatoryValueMissed(component, "Domain name");
                continue;
            }
            if (StringUtils.isEmpty(component.getString("name"))) {
                report.mandatoryValueMissed(component, "Component name");
                continue;
            }
            String publicDomainId = getPublicDomainId(domain);
            String pageTitle = "Tech of " + publicDomainId + "." + component.getString("name");
            String parentTitle = "Tech of " + publicDomainId;

            ConfluencePage page = confluencePageProvider.get();
            confluencePageList.add(page);
            page.setElement(component);
            page.setElementType("component");
            page.setTitle(pageTitle);
            page.setParentTitle(parentTitle);
            page.setType("component");
            page.setTemplate("componentPage.ftlh");
            page.setDirectoryPath(department + "/" + domain);
            page.setFileName(componentId);

            // language =========================================
            String languages = LanguageUtils.getLanguagesAsString(graph, component);
            if (!StringUtils.isEmpty(languages)) {
                page.addDataModel("language", languages);
            }

            // framework =========================================
            String framework = (String) JsonPointer.from("/details/framework").queryJson(component);
            if (framework != null && !framework.isEmpty()) {
                List<String> frameworks = Arrays.asList(framework.split("\\s*,\\s*"));
                page.addDataModel("framework", String.join(", ", TechNormalizationHelper.normalizeTechs(frameworks)));
            }

            // artifacts =========================================
            List<JsonObject> artifacts =
                    V(componentId).out("module").order().by("id").toList();
            page.addDataModel("artifacts", artifacts);

            // directDependencies ================================
            GraphTraversal<JsonObject, JsonObject> directTraversal =
                    V(componentId).out("module")
                            .outE("dependency")
                            .has("transitive", neq(true))   // Matches both null (for java) and false (for golang)
                            .has("scope", neq("test"))
                            .has("component", componentId)
                            .inV().dedup();

            List<JsonObject> directDependencies =
                    directTraversal.clone().order().by("id").toList();
            page.addDataModel("directDependencies", directDependencies);

            // transitiveDependencies ============================
            List<JsonObject> transitiveDependencies;
            if (LanguageUtils.hasLanguage(graph, component, "GoLang")) {
                // Golang
                transitiveDependencies = V(componentId).out("module")
                        .outE("dependency")
                        .has("transitive", eq(true))
                        .has("component", componentId)
                        .inV().dedup()
                        .order().by("id").toList();
            } else {
                // Java
                transitiveDependencies = directTraversal.clone()
                        .outE("dependency")
                        .has("scope", neq("test"))
                        .has("component", componentId)
                        .inV().dedup()
                        .repeat(
                                outE("dependency")
                                        .has("scope", neq("test"))
                                        .has("component", componentId)
                                        .inV().dedup()
                        ).emit().dedup()
                        .order().by("id").toList();
            }
            page.addDataModel("transitiveDependencies", transitiveDependencies);

            // groupIdDuplicated ===============================================================================
            List<Object> groupIdDuplicated = directTraversal.clone()
                    .repeat(
                            outE("dependency")
                                    .has("scope", neq("test"))
                                    .has("component", componentId)
                                    .inV().dedup())
                    .emit().dedup()
                    .values("groupId", "version").dedup()
                    .group().by("groupId").by("version")
                    .unfold().by(both)
                    .where(value().size().is(gt(1)))
                    .order().by(key)
                    .toList();
            page.addDataModel("groupIdDuplicated", groupIdDuplicated);

            addOpenApiSpecification(page, component);

            // outDependenciesList ===============================================================================
            List<Map<String, Object>> outDependenciesList = V(componentId)
                    .outE().has("type", without("module", "info", "implemented")).type().as("type")
                    .inV().where(in().hasType("domain"))
                    .<Map<String, String>>values("name", "id", "/details/domain", "/details/owner").as("component")
                    .select("component", "type").dedup().toList();
            page.addDataModel("outDependenciesList", outDependenciesList);

            // inDependenciesList ===============================================================================
            List<Map<String, Object>> inDependenciesList = V(componentId)
                    .inE().has("type", without("module", "info", "implemented")).type().as("type")
                    .outV().where(in().hasType("domain"))
                    .<Map<String, String>>values("name", "id", "/details/domain", "/details/owner").as("component")
                    .select("component", "type").dedup().toList();
            page.addDataModel("inDependenciesList", inDependenciesList);

            // gateways =========================================================================================
            List<Object> supportedGatewayList = V(componentId).out().hasType("gateway").value("id").toList();
            if (CollectionUtils.isNotEmpty(supportedGatewayList)) {
                page.addDataModel("gateways", supportedGatewayList);
            }

            // errorCodesList ===================================================================================
            List<JsonObject> directErrorCodes = V(componentId)
                    .out("errorCode").toList();
            page.addDataModel("directErrorCodes", directErrorCodes);
            List<JsonObject> indirectErrorCodes = V(componentId)
                    .repeat(
                            out("optional", "mandatory", "library")
                                    .hasNotId(componentId)
                                    .where(in().hasType("domain"))
                    ).emit().dedup()
                    .out("errorCode").toList();

            List<Object> arguments = new ArrayList<>();
            arguments.add(department);
            arguments.add(domain);
            arguments.add(componentId);

            TemplateMethodModelEx diagramMicroserviceMethod = new DiagramMicroserviceMethod(diagramService);
            String encodedText = buildDiagramImageURL(diagramMicroserviceMethod, arguments);
            if (encodedText != null) {
                page.addDataModel("encodedDiagramMicroservice", encodedText);
            }

            page.addDataModel("indirectErrorCodes", indirectErrorCodes);
            page.addDataModel("totalErrorCodesCount", directErrorCodes.size() + indirectErrorCodes.size());
        }
        return confluencePageList;
    }

    private void addOpenApiSpecification(ConfluencePage page, JsonObject component) {
        if (!BACKEND_TYPES.contains(component.getString("type"))) {
            return;
        }

        String openApiSpecPath = component.getString("openApiSpecPath");
        if (openApiSpecPath == null) {
            return;
        }

        try {
            OpenApi3 parser = new OpenApi3Parser().parse(new File(openApiSpecPath), false);
            page.addDataModel("openApi", parser);

        } catch (ResolutionException | ValidationException e) {
            LOG.warn("Can't parse OpenApi spec for " + component.getString("id"));
            report.exceptionThrown(component, e);
        }
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

}
