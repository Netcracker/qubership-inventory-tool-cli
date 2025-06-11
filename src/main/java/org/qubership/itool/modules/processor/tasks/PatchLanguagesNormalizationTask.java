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

package org.qubership.itool.modules.processor.tasks;

import org.qubership.itool.modules.graph.Graph;
import org.qubership.itool.modules.processor.InvalidGraphException;
import org.qubership.itool.modules.report.GraphReport;
import org.qubership.itool.utils.JsonUtils;
import org.qubership.itool.utils.LanguageUtils;
import org.qubership.itool.utils.TechNormalizationHelper;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.qubership.itool.modules.graph.Graph.F_ID;
import static org.qubership.itool.modules.graph.Graph.F_NAME;
import static org.qubership.itool.modules.graph.Graph.F_VERSION;
import static org.qubership.itool.modules.graph.Graph.V_DOMAIN;
import static org.qubership.itool.modules.graph.Graph.V_ROOT;
import static org.qubership.itool.utils.LanguageUtils.JAVA_LANGUAGE_NAME;
import static org.qubership.itool.utils.LanguageUtils.LANGUAGE_PATH_POINTER;

/**
 * <h2>Convert language data model to new format</h2>
 * <p>Old data model:</p>
 * <p>Input edges:</p>
 * <ol>
 * <li>(Information -&gt; Language) id: &lt;ID&gt;</li>
 * <li>(Component -&gt; Language) id: &lt;ID&gt;, type: "info"</li>
 * </ol>
 * <p>Output edges: not provided</p>
 * <br/>
 * <table border="1">
 * <caption>Old model</caption>
 * <tr><th>Property</th><th>Type</th><th>Mandatory</th><th>Description</th></tr>
 * <tr><td>id</td><td>String</td><td>yes</td><td>Unique Id</td></tr>
 * <tr><td>type</td><td>String</td><td>yes</td><td>Value is "language"</td></tr>
 * <tr><td>name</td><td>String</td><td>yes</td><td>Language name &amp; target (or build if target not known) version</td></tr>
 * <tr><td>version</td><td>Object</td><td>no</td><td></td></tr>
 * <tr><td>version.build</td><td>String</td><td>no</td><td>Language build version</td></tr>
 * <tr><td>version.source</td><td>String</td><td>no</td><td>Language source version</td></tr>
 * <tr><td>version.target</td><td>String</td><td>no</td><td>Language target version</td></tr>
 * </table>
 * <br/>
 * <p>New data model:</p>
 * <p>Input edges:</p>
 * <ol>
 * <li>(Information -&gt; Language) id: &lt;ID&gt;</li>
 * <li>(Component -&gt; Language) id: &lt;ID&gt;, type: "info", usage: ["source", "target", "etc."]</li>
 * </ol>
 * <p>Output edges: not provided</p>
 * <br/>
 * <table border="1">
 * <caption>New model</caption>
 * <tr><th>Property</th><th>Type</th><th>Mandatory</th><th>Description</th></tr>
 * <tr><td>id</td><td>String</td><td>yes</td><td>Unique Id (language name + version)</td></tr>
 * <tr><td>type</td><td>String</td><td>yes</td><td>Value is "language"</td></tr>
 * <tr><td>name</td><td>String</td><td>yes</td><td>Normalized Language name</td></tr>
 * <tr><td>version</td><td>String</td><td>no</td><td>Language version</td></tr>
 * </table>
 */
public class PatchLanguagesNormalizationTask implements GraphProcessorTask {

    private static final Logger LOG = LoggerFactory.getLogger(PatchLanguagesNormalizationTask.class);
    private static final JsonPointer LANGUAGE_SOURCE_VERSION_POINTER = JsonPointer.from("/version/source");
    private static final JsonPointer LANGUAGE_TARGET_VERSION_POINTER = JsonPointer.from("/version/target");

    /** {@inheritDoc} */
    @Override
    public void process(Graph graph) throws InvalidGraphException {
        // In version 3 languages handling was updated
        if (graph.getGraphVersion() >= 3) {
            LOG.debug("Skipping task {}, because graph with version {} should have correct language model", getClass().getSimpleName(), graph.getGraphVersion());
            return;
        }

        long startTime = System.nanoTime();
        LOG.debug("Starting task {}", getClass().getSimpleName());

        List<JsonObject> existingComponents = graph.traversal().V(V_ROOT)
                .out().hasType(V_DOMAIN)
                .out()
                .toList();
        for (JsonObject component: existingComponents) {
            // Convert "/details/language" of each component in graph to new format
            convertDetailsLanguagesProperties(graph, component);

            // Use ParsePomFile logic to get target and source versions of Java out of attached pom files in graph
            LanguageUtils.updateDetailsLanguagesUsingPomFile(graph, component);
            // Other languages don't support usages in this version, so parsing of specific language related files is not required
        }
        // Remove all the vertices of languages for graph with version less than 3 (<3).
        // Retain the language data (version.source and version.target) back to details of the component in case if pom
        // files are absent due to old version
        List<Map<String, JsonObject>> languageVertices = graph.traversal().V(V_ROOT)
                .out().hasType(V_DOMAIN)
                .out().as("C")
                .out().hasType("language").as("L")
                .<JsonObject>select("C", "L").toList();
        for (Map<String, JsonObject> languageEntry : languageVertices) {
            JsonObject languageVertex = enrichComponentLanguageUsages(languageEntry);
            graph.removeVertex(languageVertex);
        }

        // Recreate language vertices from component properties
        for (JsonObject component: existingComponents) {
            LanguageUtils.buildLanguageVerticesWithEdges(graph, component);
        }

        long endTime = System.nanoTime();
        LOG.debug("Task completed in {} ", Duration.ofNanos(endTime - startTime));
    }

    /**
     * Enrich the details of the component using data from the language vertex. That information should be sufficient
     * in case of merging of components into the app graph.
     * @param languageEntry Gremlin query result with component JsonObject in "C" and language vertex content in "L"
     * @return language vertex content
     */
    private static JsonObject enrichComponentLanguageUsages(Map<String, JsonObject> languageEntry) {
        JsonObject component = languageEntry.get("C");
        List<JsonObject> detailsLanguages = JsonUtils.asList(LANGUAGE_PATH_POINTER.queryJson(component));
        JsonObject languageVertex = languageEntry.get("L");
        JsonObject languageVertexNameVersionJson = TechNormalizationHelper.normalizeTechAsJson(languageVertex.getString(F_NAME));
        if (languageVertexNameVersionJson != null && JAVA_LANGUAGE_NAME.equals(languageVertexNameVersionJson.getString(F_NAME))) {
            String vertexSourceVersion = (String) LANGUAGE_SOURCE_VERSION_POINTER.queryJson(languageVertex);
            String vertexTargetVersion = (String) LANGUAGE_TARGET_VERSION_POINTER.queryJson(languageVertex);
            if (detailsLanguages == null) {
                // Absence of original data for languages may happen in inconsistent test data, or very strange corner cases
                // Trying to recreate the language using data from vertex
                detailsLanguages = JsonUtils.asList(LanguageUtils.convertListToLanguages(languageVertex.getString(F_NAME)));
                JsonObject detailsLanguage = detailsLanguages.get(0);
                LANGUAGE_PATH_POINTER.writeJson(component, new JsonArray(detailsLanguages), true);
                updateVersionUsage(vertexSourceVersion, vertexTargetVersion, detailsLanguage);
            } else {
                // Only processing java languages with missing usages because usages were not populated from pom file
                // but were found in the language vertex, meaning the pom file is not available in graph.
                detailsLanguages.stream()
                        .filter(detailsLanguage -> detailsLanguage.getJsonArray("usage") == null
                                && detailsLanguage.getString(F_NAME).equals(languageVertexNameVersionJson.getString(F_NAME)))
                        .forEach(detailsLanguage -> updateVersionUsage(vertexSourceVersion, vertexTargetVersion, detailsLanguage));
            }
        }
        return languageVertex;
    }

    private static void updateVersionUsage(String vertexSourceVersion, String vertexTargetVersion, JsonObject detailsLanguage) {
        String detailsLanguageVersion = detailsLanguage.getString(F_VERSION);
        if (detailsLanguageVersion == null) {
            return;
        }
        if (detailsLanguageVersion.equals(vertexSourceVersion)) {
            LanguageUtils.updateVersionUsage("source", detailsLanguage);
        }
        if (detailsLanguageVersion.equals(vertexTargetVersion)) {
            LanguageUtils.updateVersionUsage("target", detailsLanguage);
        }
    }

    /**
     * <h1>Convert "/details/language" of a component in graph to new format </h1>
     * <p>Old format:</p>
     * <p>Comma-separated string</p>
     * <p>"Java 21, TypeScript"</p>
     * <p>New format:</p>
     * <p>JsonArray</p>
     * <p>"language" : [ {"name" : "Java", "version" : "21", "usage" : [ "source", "target" ]}, {"name" : "TypeScript"} ]</p>
     *
     * @param graph graph
     * @param component component to perform conversion
     */
    private void convertDetailsLanguagesProperties(Graph graph, JsonObject component) {
        Object languages = LANGUAGE_PATH_POINTER.queryJson(component);
        JsonArray resultingList = new JsonArray();
        if (languages instanceof JsonArray || languages instanceof List) {
            List<Object> languageList = JsonUtils.asList(languages);
            for (Object languageObj : languageList) {
                try {
                    // Assuming that we are dealing with new format of entries in the list, otherwise the error will be logged
                    Map<String, JsonObject> languageJson = JsonUtils.asMap(languageObj);
                    resultingList.add(JsonObject.mapFrom(languageJson));
                } catch (ClassCastException classCastException) {
                    GraphReport report = graph.getReport();
                    report.addMessage(GraphReport.CONF_ERROR, component, classCastException.getMessage());
                }
            }
            if (languageList.size() != resultingList.size()) {
                LANGUAGE_PATH_POINTER.writeJson(component, resultingList);
            } else {
                LOG.debug("{}: Language of the component has correct format already", component.getString(F_ID));
            }
        } else if (languages instanceof String) {
            resultingList = LanguageUtils.convertListToLanguages(languages);
            LANGUAGE_PATH_POINTER.writeJson(component, resultingList);
        }
    }


}
