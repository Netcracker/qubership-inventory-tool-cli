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
import org.qubership.itool.modules.diagram.DiagramService;
import org.qubership.itool.modules.template.ConfluencePage;
import org.qubership.itool.modules.template.DiagramGeneralDomainMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Provider;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.qubership.itool.modules.gremlin2.P.eq;
import static org.qubership.itool.modules.gremlin2.graph.__.count;
import static org.qubership.itool.modules.gremlin2.graph.__.has;
import static org.qubership.itool.modules.gremlin2.graph.__.in;
import static org.qubership.itool.modules.gremlin2.graph.__.out;

public class ConfluenceGenerateDepartmentMainPageVerticle extends AbstractConfluenceGenerationPageVerticle {
    protected Logger LOG = LoggerFactory.getLogger(ConfluenceGenerateDepartmentMainPageVerticle.class);

    @Resource
    protected DiagramService diagramService;
    @Resource
    protected Provider<ConfluencePage> confluencePageProvider;

    @Override
    protected List<ConfluencePage> prepareConfluencePageList(String department) {
        List<ConfluencePage> confluencePageList = new ArrayList<>();

        List<String> domainIdList =
                V().hasType("domain").has("department", eq(department)).id().toList();

        ConfluencePage page = confluencePageProvider.get();
        confluencePageList.add(page);

        page.setTitle("Tech of cloud " + department);
        page.setParentTitle("Cloud: Documentation");
        page.setType("department");
        page.setTemplate("departmentPage.ftlh");
        page.setDirectoryPath(department);
        page.setFileName("departmentPage");

        // domainsList ============================================================================
        List<Map<String, Object>> domainsList =
                V(domainIdList).as("D")
                        .local(out().type().count()).as("T")
                        .local(out().type().group().by(count()))
                        .as("C").select("D", "T", "C").toList();
        page.addDataModel("domainsList", domainsList);

        // techStackList ==========================================================================
        List<Map<Object, Object>> techStackList =
                V(domainIdList).as("D").out()
                        .both().where(in().hasId("Infra", "Info")).not(has("type", "gateway"))
                        .value("name").as("T").select("T", "D")
                        .dedup()
                        .group().by("T").by("D").toList();
        page.addDataModel("techStackList", techStackList);

        // tmfSpecsList ===========================================================================
        List<Map<String, Object>> tmfSpecsList = V(domainIdList).as("D").out().as("C")
                .out().where(in().hasId("Spec")).as("S")
                .select("D", "C", "S").toList();
        page.addDataModel("tmfSpecsList", tmfSpecsList);

        List<Object> arguments = new ArrayList<>();
        arguments.add(department);
        TemplateMethodModelEx diagramGeneralDomainMethod = new DiagramGeneralDomainMethod(diagramService);
        String encodedText = buildDiagramImageURL(diagramGeneralDomainMethod, arguments);
        if (encodedText != null) {
            page.addDataModel("encodedDiagramDepartment", encodedText);
        }

        return confluencePageList;
    }


    @Override
    protected Logger getLogger() {
        return LOG;
    }

}
