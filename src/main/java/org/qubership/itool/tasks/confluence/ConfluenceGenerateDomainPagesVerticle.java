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
import org.qubership.itool.modules.diagram.DiagramService;
import org.qubership.itool.modules.template.ConfluencePage;
import org.qubership.itool.modules.template.DiagramDomainMethod;
import org.qubership.itool.modules.template.DiagramGeneralDomainMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Provider;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.qubership.itool.modules.graph.Graph.F_ID;
import static org.qubership.itool.modules.gremlin2.P.eq;
import static org.qubership.itool.modules.gremlin2.P.within;
import static org.qubership.itool.modules.gremlin2.graph.__.has;
import static org.qubership.itool.modules.gremlin2.graph.__.hasType;
import static org.qubership.itool.modules.gremlin2.graph.__.in;
import static org.qubership.itool.modules.gremlin2.graph.__.not;
import static org.qubership.itool.modules.gremlin2.graph.__.out;


public class ConfluenceGenerateDomainPagesVerticle extends AbstractConfluenceGenerationPageVerticle {
    protected Logger LOG = LoggerFactory.getLogger(ConfluenceGenerateDomainPagesVerticle.class);

    @Resource
    protected DiagramService diagramService;
    @Resource
    protected Provider<ConfluencePage> confluencePageProvider;

    @Override
    protected List<ConfluencePage> prepareConfluencePageList(String department) {
        List<ConfluencePage> confluencePageList = new ArrayList<>();

        List<JsonObject> domainIdList = V().hasType("domain").has("department", eq(department)).toList();

        for (JsonObject domain : domainIdList) {
            String domainId = domain.getString(F_ID);

            ConfluencePage page = confluencePageProvider.get();
            confluencePageList.add(page);

            page.setElement(domain);
            page.setElementType("domain");
            page.setTitle("Tech of " + getPublicDomainId(domainId));
            page.setParentTitle("Tech of cloud " + department);
            page.setType("domain");
            page.setTemplate("domainPage.ftlh");
            page.setDirectoryPath(department + "/" + domainId);
            page.setFileName("domainPage");

            // components =====================================================
            List<Map<String, JsonObject>> components = V(domainId).out().as("C")
                    .local(in().where(not(hasType("domain"))).count()).as("T")
                    .<JsonObject>select("C", "T")
                    .toList();
            page.addDataModel("components", components);

            // techStack ======================================================
            List<String> techStack =
                    V(domainId).out().out().where(in().hasId("Infra", "Info")).not(has("type", "gateway")).name().dedup().order().toList();
            page.addDataModel("techStack", techStack);

            // techStackByService =============================================
            List<Map<Object, Object>> techStackByService = V(domainId).out().as("C")
                    .local(
                            out().where(in().hasId("Infra", "Info")).not(has("type", "gateway")).name().dedup().fold()
                    ).as("T")
                    .select("C", "T")
                    .values("abbreviation:/C/details/abbreviation", "name:/C/name", "apiSpec:/C/details/api/apiSpecPublished", "usedTech:/T")
                    .order().by("name")
                    .toList();
            page.addDataModel("techStackByService", techStackByService);

            // tmfSpec =========================================================
            List<Map<String, Object>> tmfSpecList = V(domainId).out().as("C")
                    .out().where(in().hasId("Spec")).as("S")
                    .select("C", "S")
                    .toList();
            page.addDataModel("tmfSpecs", tmfSpecList);

            // blueGreenReady =================================================================
            List<Map<Object, String>> blueGreenReady = V(domainId).out().hasType("backend")
                    .not(has("/details/deploymentConfiguration/deployOptions/bluegreen", eq(true)))
                    .<String>values("id", "/details/abbreviation", "name")
                    .toList();

            page.addDataModel("blueGreenReady", blueGreenReady);

            // faultToleranceSupport ===========================================================
            List<Map<Object, Object>> faultToleranceSupport =
                    V(domainId).out().values("id", "name", "/details/abbreviation", "/features/faultTolerance").as("C")
                            .value("faultTolerance")
                            .union(
                                    has("errorCodes", within("not required", "no")).key().is(eq("errorCodes")).as("T")
                                    , has("httpRetryPolicy", within("not required", "no")).key().is(eq("httpRetryPolicy")).as("T")
                                    , has("customHealthProbes", within("not required", "no")).key().is(eq("customHealthProbes")).as("T")
                            )
                            .select("C", "T")
                            .group().by("C").by("T")
                            .toList();


            List<Object> arguments = new ArrayList<>();
            arguments.add(department);
            arguments.add(domainId);
            TemplateMethodModelEx diagramGeneralDomainMethod = new DiagramGeneralDomainMethod(diagramService);
            String encodedText = buildDiagramImageURL(diagramGeneralDomainMethod, arguments);
            if (encodedText != null) {
                page.addDataModel("encodedDiagramGeneralDomain", encodedText);
            }
            TemplateMethodModelEx diagramDomainMethod = new DiagramDomainMethod(diagramService);
            encodedText = buildDiagramImageURL(diagramDomainMethod, arguments);
            if (encodedText != null) {
                page.addDataModel("encodedDiagramDomain", encodedText);
            }

            page.addDataModel("faultToleranceSupport", faultToleranceSupport);
        }

        return confluencePageList;
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

}
