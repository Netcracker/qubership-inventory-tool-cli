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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.inject.Provider;

import javax.annotation.Resource;

import static org.qubership.itool.modules.gremlin2.graph.__.*;

public class ConfluenceSummaryLibrariesVerticle extends AbstractConfluenceGenerationPageVerticle {
    protected Logger LOG = LoggerFactory.getLogger(ConfluenceSummaryLibrariesVerticle.class);

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

        page.setTitle("Cloud Libraries list");
        page.setParentTitle("Summary");
        page.setType("summary");
        page.setTemplate("summary/librariesList.ftlh");
        page.setDirectoryPath("summary/");
        page.setFileName("libraries");

        // librariesList =======================================================
        List<Map<String, JsonObject>> librariesList = V().hasType("domain").as("D")
            .out().as("C").hasType("library")
            .local(in().where(not(hasType("domain"))).count()).as("T")
            .<JsonObject>select("D", "C", "T").toList();
        page.addDataModel("librariesList", librariesList);
        return confluencePageList;
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

}
