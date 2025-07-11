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

package org.qubership.itool.tasks.confluence.summary;

import org.qubership.itool.modules.template.ConfluencePage;
import org.qubership.itool.tasks.confluence.AbstractConfluenceGenerationPageVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;
import org.apache.commons.lang3.StringUtils;
import org.qubership.itool.modules.gremlin2.P;
import org.qubership.itool.utils.JsonUtils;
import org.qubership.itool.utils.LanguageUtils;
import org.qubership.itool.utils.TechNormalizationHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Provider;

import javax.annotation.Resource;

import java.util.*;

import static org.qubership.itool.modules.graph.Graph.F_MICROSERVICE_FLAG;
import static org.qubership.itool.modules.graph.Graph.F_VERSION;
import static org.qubership.itool.modules.graph.Graph.V_DOMAIN;
import static org.qubership.itool.modules.graph.Graph.V_ROOT;
import static org.qubership.itool.modules.gremlin2.graph.__.*;

public class ConfluenceSummaryMicroservicesVerticle extends AbstractConfluenceGenerationPageVerticle {
    protected Logger LOG = LoggerFactory.getLogger(ConfluenceSummaryMicroservicesVerticle.class);

    @Resource
    protected Provider<ConfluencePage> confluencePageProvider;

    @Override
    protected List<ConfluencePage> prepareConfluencePageList(String department) {
        return null; // do nothing
    }

    @Override
    protected List<ConfluencePage> prepareConfluencePageList() {
        List<ConfluencePage> confluencePageList = new ArrayList<>();

        ConfluencePage page = confluencePageProvider.get();
        confluencePageList.add(page);

        page.setTitle("Cloud Microservices list");
        page.setParentTitle("Summary");
        page.setType("summary");
        page.setTemplate("summary/microservicesList.ftlh");
        page.setDirectoryPath("summary/");
        page.setFileName("microservices");

        // microservicesList =======================================================
        List<Map<String, Object>> microservicesList = graph.traversal().V(V_ROOT).out().hasType(V_DOMAIN).as("D")
                .out().not(hasType("library"))
                .has(F_MICROSERVICE_FLAG, P.eq(true)).as("C")
                .local(in().where(not(hasType("domain"))).count()).as("T")
                .local(outE("info").as("LU")
                        .inV().hasType("language").as("LV")
                        .select("LU", "LV")
                        .values("usages:/LU/usage", "id:/LV/id", "name:/LV/name", "version:/LV/version").fold())
                .as("L")
                .select("D", "C", "T", "L").toList();

        for (Map<String, Object> microservice : microservicesList) {

            JsonObject component = (JsonObject) microservice.get("C");

            String languages = LanguageUtils.getLanguagesAsString(graph, component);
            if (!StringUtils.isEmpty(languages)) {
                microservice.put("language",  languages);
            }

            String framework = (String) JsonPointer.from("/details/framework").queryJson(component);
            if (framework != null && !framework.isEmpty()) {
                List<String> frameworks = Arrays.asList(framework.split("\\s*,\\s*"));
                microservice.put("framework", String.join(", ", TechNormalizationHelper.normalizeTechs(frameworks)));
            }

            List<Object> versions = JsonUtils.asList(microservice.get("L"));

            List<String> sources = new ArrayList<>();
            List<String> targets = new ArrayList<>();
            for (Object languageVersion : versions) {
                Map<String, String> versionDetails = JsonUtils.asMap(languageVersion);
                List<String> languageUsages = JsonUtils.asList(versionDetails.get("usages"));
                if (languageUsages != null) {
                    updateUsages(versionDetails, languageUsages, sources, "source");
                    updateUsages(versionDetails, languageUsages, targets, "target");
                }
            }
            JsonObject versionsList = new JsonObject();
            versionsList.put("source", String.join(", ", sources));
            versionsList.put("target", String.join(", ", targets));
            microservice.remove("L");
            microservice.put("version", versionsList);
        }
        page.addDataModel("microservicesList", microservicesList);
        return confluencePageList;
    }
    private static void updateUsages(Map<String, String> languageVersion, List<String> languageUsages, List<String> usageContainer, String usage) {
        if (languageUsages.contains(usage)) {
            String version = languageVersion.get(F_VERSION);
            if (version != null) {
                usageContainer.add(version);
            }
        }
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

}
