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

package org.qubership.itool.tasks.confluence.report;

import org.qubership.itool.modules.template.ConfluencePage;
import org.qubership.itool.tasks.confluence.AbstractConfluenceGenerationPageVerticle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Provider;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

public class ConfluenceGenerateReportErrorsVerticle extends AbstractConfluenceGenerationPageVerticle {
    protected Logger LOG = LoggerFactory.getLogger(ConfluenceGenerateReportErrorsVerticle.class);

    @Resource
    protected Provider<ConfluencePage> confluencePageProvider;

    @Override
    protected List<ConfluencePage> prepareConfluencePageList(String department) {
        return null;
    }

    @Override
    protected List<ConfluencePage> prepareConfluencePageList() {
        List<ConfluencePage> confluencePageList = new ArrayList<>();
        ConfluencePage page = confluencePageProvider.get();
        confluencePageList.add(page);

        page.setTitle("Processing errors");
        page.setParentTitle("Reports");
        page.setType("report");
        page.setTemplate("reports/errors.ftlh");
        page.setDirectoryPath("reports/");
        page.setFileName("errors");

        List<?> errors = report.dumpRecords(false).getList();
        page.addDataModel("errors", errors);

        return confluencePageList;
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

}
