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

package org.qubership.itool.tasks.html.report;

import io.vertx.core.json.JsonObject;
import org.qubership.itool.modules.diagram.DiagramService;
import org.qubership.itool.modules.template.ConfluencePage;
import org.qubership.itool.tasks.html.AbstractHtmlGenerationPageVerticle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.qubership.itool.modules.graph.Graph.V_DOMAIN;
import static org.qubership.itool.modules.graph.Graph.V_ROOT;
import static org.qubership.itool.modules.gremlin2.graph.__.has;
import static org.qubership.itool.modules.gremlin2.graph.__.hasType;
import static org.qubership.itool.modules.gremlin2.graph.__.in;
import static org.qubership.itool.modules.gremlin2.graph.__.not;
import static org.qubership.itool.modules.gremlin2.graph.__.out;


public class HtmlGenerateReportPageVerticle extends AbstractHtmlGenerationPageVerticle {
    protected Logger LOG = LoggerFactory.getLogger(HtmlGenerateReportPageVerticle.class);

    @Resource
    protected DiagramService diagramService;

    @Override
    protected List<ConfluencePage> preparePageList(String department) {
        return null; // do nothing
    }
    @Override
    protected List<ConfluencePage> preparePageList() {
        List<ConfluencePage> generatedPageList = new ArrayList<>();
        ConfluencePage page = new ConfluencePage();

        page.setTitle("HTML report");
        page.setParentTitle("Reports");
        page.setDirectoryPath("reports/");
        page.setType("htmlReport");
        page.setTemplate("htmlReportPage.ftlh");
        page.setFileName("htmlReportPage");

        generatedPageList.add(page);

        // components =====================================================
        List<Map<String, JsonObject>> components = V(V_ROOT).out().hasType(V_DOMAIN)
                .out().as("C")
                .local(in().where(not(hasType("domain"))).count()).as("T")
                .local(out().where(in().hasId("Infra", "Info")).not(has("type", "gateway")).name().dedup().fold()).as("TECH")
                .<JsonObject>select("C", "T", "TECH")
                .toList();
        page.addDataModel("components", components);
        List<String> headers = List.of();
        if (!components.isEmpty()) {
            JsonObject component = components.get(0).get("C");
            JsonObject validationResults = component.getJsonObject("validationResults");
            headers = new ArrayList<>(validationResults.getMap().keySet());
        }
        page.addDataModel("reportHeaders", headers);
        return generatedPageList;
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }
}
