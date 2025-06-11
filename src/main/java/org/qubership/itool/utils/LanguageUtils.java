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

package org.qubership.itool.utils;

import org.qubership.itool.modules.graph.BasicGraph;
import org.qubership.itool.modules.graph.Graph;
import org.qubership.itool.modules.graph.GraphDataConstants;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static org.qubership.itool.modules.graph.Graph.F_ID;
import static org.qubership.itool.modules.graph.Graph.F_NAME;
import static org.qubership.itool.modules.graph.Graph.F_TYPE;
import static org.qubership.itool.modules.graph.Graph.F_VERSION;
import static org.qubership.itool.modules.graph.Graph.P_DETAILS_LANGUAGE;

public class LanguageUtils {

    public static final JsonPointer LANGUAGE_PATH_POINTER = JsonPointer.from(P_DETAILS_LANGUAGE);
    private static final XPathFactory xPathfactory = XPathFactory.newInstance();
    private static final XPath xpath = xPathfactory.newXPath();
    private final static List<String> USAGES_LIST = List.of("source", "target", "release");
    public final static String JAVA_LANGUAGE_NAME = "Java";
    private final static String F_LANGUAGE_NAME = "name";
    private final static String F_LANGUAGE_VERSION = "version";
    private final static String F_LANGUAGE_USAGE = "usage";

    public static JsonArray convertListToLanguages(Object value) {
        JsonArray languages = new JsonArray();
        if (value instanceof JsonArray) {
            value = ((JsonArray)value).getList();
        } else if (value instanceof String) {
            value = List.of(((String) value).split("\\s*,\\s*"));
        }
        if (value instanceof List) {
            ((List<Object>)value).stream()
                    .map(String::valueOf)
                    .filter(s -> ! GraphDataConstants.NOS_TO_RECOGNIZE.contains(s))
                    .map(TechNormalizationHelper::normalizeTechAsJson)
                    .filter(Objects::nonNull)
                    .forEach(languages::add);
        }
        return languages;
    }

    public static boolean hasLanguage(BasicGraph graph, JsonObject component, String languageName) {
        return graph.traversal().V(component.getString(F_ID))
                .out("info")
                .hasType("language")
                .has(F_NAME, languageName).next() != null;
    }

    public static String getLanguagesAsString(BasicGraph graph, JsonObject component) {
        List<Map<Object, String>> languages = graph.traversal().V(component.getString(F_ID))
                .out("info")
                .hasType("language")
                .<String>values(F_NAME, F_VERSION).toList();

        return languages.stream()
                .map(lng -> {
                    String version = lng.get(F_VERSION);
                    if (version == null) {
                        return lng.get(F_NAME);
                    }
                    return lng.get(F_NAME) + " " + version;
                }).collect(Collectors.joining(", "));
    }

    /**
     * Takes contents of pom.xml file from graph, parses it and stores the results of the parse in /details/language structure
     * See <a href="https://maven.apache.org/plugins/maven-compiler-plugin/examples/set-compiler-source-and-target.html">this article</a>
     * for more details on setting java versions for maven compiler plugin
     * @param graph graph
     * @param component component to be processed
     */
    public static void updateDetailsLanguagesUsingPomFile(Graph graph, JsonObject component) {
        String pomFileContents = (String) graph.traversal().V(component.getString("id")).out()
                .glob("pom.xml").value("content").next();
        if (pomFileContents == null) {
            return;
        }
        Document document;
        try {
            document = XmlParser.parseXmlString(pomFileContents, "pom.xml");
            Map<String, String> properties = extractProperties(document);
            NodeList mavenCompilerPlugin = findMavenCompilerPlugin(document);
            // fetch the versions
            Map<String, String> javaUsageVersions = new HashMap<>();
            for (String languageUsage : USAGES_LIST) {
                String version = getVersion(mavenCompilerPlugin, properties, languageUsage);
                if (!StringUtils.isEmpty(version)) {
                    javaUsageVersions.put(languageUsage, version);
                }
            }

            String javaVersion = ObjectUtils.firstNonNull(javaUsageVersions.values().toArray(new String[0]));
            //update component /details/language
            if (javaVersion != null) {
                Object versionsObj = LANGUAGE_PATH_POINTER.queryJson(component);
                List<JsonObject> versions = JsonUtils.asList(versionsObj);
                if (versions == null) {
                    versions = new ArrayList<>();
                }

                List<JsonObject> languageVersions = versions;
                for (String javaUsage : javaUsageVersions.keySet()) {
                    AtomicBoolean versionFound = new AtomicBoolean(false);
                    languageVersions = languageVersions.stream()
                            .map(version -> updateJavaVersions(javaUsage, version, javaUsageVersions.get(javaUsage), versionFound))
                            .collect(Collectors.toList());
                    if (!versionFound.get()) {
                        languageVersions.add(new JsonObject()
                                .put(F_LANGUAGE_NAME, JAVA_LANGUAGE_NAME)
                                .put(F_LANGUAGE_VERSION, javaVersion)
                                .put(F_LANGUAGE_USAGE, new JsonArray().add(javaUsage)));
                    }
                }

                LANGUAGE_PATH_POINTER.writeJson(component, new JsonArray(languageVersions), true);
            }
        } catch (Exception e) {
            graph.getReport().exceptionThrown(component, e);
        }
    }

    private static NodeList findMavenCompilerPlugin(Document document) throws XPathExpressionException {
        NodeList mavenCompilerPlugin = (NodeList) xpath.compile(
                        "/project/build/pluginManagement/plugins/plugin[artifactId='maven-compiler-plugin']")
                .evaluate(document, XPathConstants.NODESET);
        if (mavenCompilerPlugin != null && mavenCompilerPlugin.getLength() > 0) {
            return mavenCompilerPlugin;
        }
        // /project/build/plugins is a valid path too
        return (NodeList) xpath.compile("/project/build/plugins/plugin[artifactId='maven-compiler-plugin']")
                .evaluate(document, XPathConstants.NODESET);
    }

    private static JsonObject updateJavaVersions(String javaUsage, JsonObject version, String javaUsageVersion, AtomicBoolean versionFound) {
        if (JAVA_LANGUAGE_NAME.equals(version.getString(F_LANGUAGE_NAME))) {
            String languageVersion = version.getString(F_LANGUAGE_VERSION);
            if (languageVersion == null) {
                // Add version with its usage
                version.put(F_LANGUAGE_VERSION, javaUsageVersion);
                version.put(F_LANGUAGE_USAGE, new JsonArray().add(javaUsage));
                versionFound.set(true);
            } else if (languageVersion.equals(javaUsageVersion)) {
                updateVersionUsage(javaUsage, version);
                versionFound.set(true);
            }
        }
        return version;
    }

    public static void updateVersionUsage(String newUsage, JsonObject version) {
        JsonArray usage = version.getJsonArray(F_LANGUAGE_USAGE);
        if (usage == null) {
            usage = new JsonArray();
        }
        if (usage.contains(newUsage)) {
            return;
        }
        usage.add(newUsage);
        version.put(F_LANGUAGE_USAGE, usage);
    }

    private static String getVersion(NodeList mavenCompilerPlugin, Map<String, String> properties, String versionType)
            throws Exception
    {
        // Search in properties first
        String version = properties.get("maven.compiler." + versionType);
        version = getValueFromVariable(properties, version);
        if (version != null) {
            return version;
        }

        if (mavenCompilerPlugin != null && mavenCompilerPlugin.getLength() > 0) {
            version = (String) xpath.compile("configuration/" + versionType)
                    .evaluate(mavenCompilerPlugin.item(0), XPathConstants.STRING);
            version = getValueFromVariable(properties, version);
        }
        if (version != null) {
            return version;
        }

        return properties.get("java.version");
    }

    private static String getValueFromVariable(Map<String, String> properties, String version) {
        if (version != null && version.startsWith("${")) {
            String varName = extractPlaceholderName(version);
            version = properties.get(varName);
        }
        return version;
    }

    private static String extractPlaceholderName(String str) {
        return str.substring(2, str.length() - 1);
    }

    private static Map<String, String> extractProperties(Document document) throws Exception {
        NodeList props = (NodeList) xpath.compile("/project/properties/*").evaluate(document, XPathConstants.NODESET);
        Map<String, String> properties = new LinkedHashMap<>(props.getLength());
        for (int i = 0; i < props.getLength(); i++) {
            Element item = (Element) props.item(i);
            String key = item.getTagName();
            Node child0 = item.getChildNodes().item(0);
            if (child0 instanceof Text) {
                String value = ((Text)child0).getWholeText();
                properties.put(key, value);
            }
        }
        return properties;
    }

    public static void buildLanguageVerticesWithEdges(Graph graph, JsonObject component) {
        Object languageProperty = LANGUAGE_PATH_POINTER.queryJson(component);
        if (languageProperty == null) {
            return;
        }
        List<JsonObject> languageProperties = JsonUtils.asList(languageProperty);

        for (JsonObject language : languageProperties) {
            String languageName = language.getString(F_NAME);
            String languageVersion = language.getString(F_VERSION);
            String languageId = languageName + (languageVersion != null ? " " + languageVersion : "");
            JsonObject languageVertex = graph.getVertex(languageId);
            if (languageVertex == null) {
                // Create new language vertex if not found
                languageVertex = new JsonObject()
                        .put(F_ID, languageId)
                        .put(F_TYPE, "language")
                        .put(F_NAME, languageName);
                if (languageVersion != null) {
                    languageVertex.put(F_VERSION, languageVersion);
                }
                graph.addVertex("Info", languageVertex);
            }
            // Add new edge to language node
            JsonObject languageEdge = new JsonObject()
                    .put("type", "info");
            JsonArray languageUsages = language.getJsonArray("usage");
            if (languageUsages != null) {
                languageEdge.put("usage", languageUsages);
            }
            graph.addEdge(component, languageVertex, languageEdge);
        }
    }
}
