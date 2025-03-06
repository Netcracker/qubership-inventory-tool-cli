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
import freemarker.template.TemplateModelException;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.qubership.itool.modules.diagram.UMLDiagramEncoder;
import org.qubership.itool.modules.graph.Graph;
import org.qubership.itool.modules.template.ConfluencePage;
import org.qubership.itool.modules.template.TemplateService;
import org.qubership.itool.tasks.AbstractAggregationTaskVerticle;
import org.qubership.itool.utils.ConfigUtils;
import org.qubership.itool.utils.JsonUtils;

import javax.annotation.Resource;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.qubership.itool.utils.ConfigProperties.PLANTUML_URL_POINTER;

public abstract class AbstractGenerationPageVerticle extends AbstractAggregationTaskVerticle {
    @Resource
    protected TemplateService templateService;

    @Override
    protected void taskStart(Promise<?> taskPromise) throws Exception {
        final JsonArray rootGeneratedPages = JsonUtils.getOrCreateJsonArray(graph.getVertex(Graph.V_ROOT), getPagesMetaLocation());

        vertx.executeBlocking(promise -> {
            List<ConfluencePage> pageList = preparePageList();
            pageList.forEach(this::setupPageProperties);
            promise.complete(pageList);
        }, res -> {
            if (res.failed()) {
                report.internalError(ExceptionUtils.getStackTrace(res.cause()));
                taskCompleted(taskPromise);
                return;
            }

            @SuppressWarnings("unchecked")
            List<ConfluencePage> pageList = (List<ConfluencePage>)res.result();
            if (pageList == null) {
                taskCompleted(taskPromise);
                return;
            }

            @SuppressWarnings("rawtypes")
            List<Future> futureList = new ArrayList<>();
            for (ConfluencePage generatedPage : pageList) {
                getLogger().info("Generate page: {}/{}", generatedPage.getDirectoryPath(), generatedPage.getFileName());

                Future<Void> future = Future.future(promise -> {
                    try {
                        String result =  templateService.processTemplate(generatedPage);
                        if (result != null) {
                            File dir = new File(getOutputPath() + generatedPage.getDirectoryPath());
                            if (!dir.exists()) {
                                dir.mkdirs();
                            }
                            String path = dir.getPath() + "/" + generatedPage.getFileName() + getPageExtension();
                            vertx.fileSystem().writeFileBlocking(path, Buffer.buffer(result));

                            JsonObject page = new JsonObject();
                            page.put("title", generatedPage.getTitle());
                            page.put("parentTitle", generatedPage.getParentTitle());
                            page.put("type", generatedPage.getType());
                            page.put("onDiskPath", path);
                            page.put("space", generatedPage.getSpace());
                            rootGeneratedPages.add(page);
                            getLogger().debug("Page '{}' generation complete ({})", page.getString("title"), page.getString("onDiskPath"));
                        }
                        promise.complete();

                    } catch (Exception ex) {
                        promise.fail(ex);
                        JsonObject component = generatedPage.getElement();
                        this.report.exceptionThrown(component, ex);
                    }
                });
                futureList.add(future);
            }

            completeCompositeTask(futureList, taskPromise);
        });
    }

    protected abstract String getPagesMetaLocation();

    protected abstract String getPageExtension();

    protected abstract String getOutputPath();

    protected String buildDiagramImageURL(TemplateMethodModelEx diagramMethod, List<Object> arguments) {
        String generatedText = null;
        try {
            generatedText = (String) diagramMethod.exec(arguments);
        } catch (TemplateModelException e) {
            getLogger().error("Could not generate plantuml diagram. StackTrace : {}", e.getMessage());
        }

        if (StringUtils.isNotEmpty(generatedText)) {
            String encodedText = UMLDiagramEncoder.encodeDiagram(generatedText);
            JsonObject config = config();
            String plantumlURL = ConfigUtils.getConfigValue(PLANTUML_URL_POINTER, config);
            encodedText = plantumlURL + encodedText;
            return encodedText;
        }
        return null;
    }

    protected List<ConfluencePage> preparePageList() {
        List<ConfluencePage> generatedPageList = new ArrayList<>();

        List<String> departments = V().hasType("domain").<String>value("department").dedup().toList();
        for (String department : departments) {
            List<ConfluencePage> confluencePages = preparePageList(department);
            if (confluencePages == null) {
                continue;
            }
            for (ConfluencePage page : confluencePages) {
                page.addDataModel("department", department);
            }
            generatedPageList.addAll(confluencePages);
        }

        return generatedPageList;
    }

    protected abstract List<ConfluencePage> preparePageList(String department);

    protected abstract void setupPageProperties(ConfluencePage page);
}
