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
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import jakarta.inject.Provider;

import org.qubership.itool.utils.JsonUtils;
import org.qubership.itool.utils.TechNormalizationHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Resource;

import static org.qubership.itool.modules.gremlin2.graph.__.has;
import static org.qubership.itool.modules.gremlin2.graph.__.in;


public class ConfluenceSummaryTechStacksPerDomainVerticle extends AbstractConfluenceGenerationPageVerticle {
    protected Logger LOG = LoggerFactory.getLogger(ConfluenceSummaryTechStacksPerDomainVerticle.class);

    @Resource
    Provider<ConfluencePage> confluencePageProvider;

    @Override
    protected List<ConfluencePage> prepareConfluencePageList(String department) {
        return null; // do nothing
    }

    @SuppressWarnings("rawtypes")
    @Override
    protected List<ConfluencePage> prepareConfluencePageList() {
        List<ConfluencePage> confluencePageList = new ArrayList<>();

        ConfluencePage page = confluencePageProvider.get();
        confluencePageList.add(page);

        page.setTitle("Cloud Tech. stack per Domain");
        page.setParentTitle("Summary");
        page.setType("summary");
        page.setTemplate("summary/techStacksPerDomain.ftlh");
        page.setDirectoryPath("summary/");
        page.setFileName("techStacksPerDomain");

        List<JsonObject> techStacks = new ArrayList<>();
        Map<String, List> techs = V().hasType("domain").as("D").out()
                .bothE().as("E").inV().where(in().hasId("Infra", "Info")).not(has("type", "gateway")).name()
                .as("T")
                .select("T", "D", "E")
                .values("domain:/D/id", "usedTech:/T", "type:/E/type")
                .dedup().<String, List>group().by("domain").next();

        for (Map.Entry<String, List> domain: techs.entrySet()) {
            JsonArray unknownTechs = new JsonArray();
            JsonObject techJson = new JsonObject();
            for (Object tech: domain.getValue()) {
                Map<String, String> techMap = JsonUtils.asMap(tech);
                String usedTech = techMap.get("usedTech");
                Optional<String> matchedTechKey = TechNormalizationHelper.normalizeTech(usedTech);
                if (matchedTechKey.isPresent()) {
                    techJson.put(matchedTechKey.get(), toNecessityValue(techMap.get("type")));
                } else {
                    unknownTechs.add(usedTech);
                }
            };
            techStacks.add(new JsonObject()
                    .put("domain", domain.getKey())
                    .put("techsList", techJson)
                    .put("unknownTechs", unknownTechs.isEmpty() ? null : unknownTechs)
            );
        };

        page.addDataModel("techStacks", techStacks);
        page.addDataModel("techNames", TechNormalizationHelper.getTechNamesMap().keySet());
        return confluencePageList;
    }

    private String toNecessityValue(String type) {
        if (type == null) {
            return "Unknown";
        }
        if ("optional".equalsIgnoreCase(type)) {
            return "O";
        }
        return "M";
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

}
