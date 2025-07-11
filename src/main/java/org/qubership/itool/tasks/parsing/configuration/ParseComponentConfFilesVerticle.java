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

package org.qubership.itool.tasks.parsing.configuration;

import org.qubership.itool.tasks.parsing.AbstractParseFileTask;
import org.apache.commons.lang3.exception.ExceptionUtils;

import io.vertx.core.json.*;
import jakarta.inject.Provider;

import org.qubership.itool.utils.FSUtils;
import org.qubership.itool.utils.GitUtils;
import org.qubership.itool.utils.YamlParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import javax.annotation.Resource;

import static org.qubership.itool.modules.report.GraphReport.EXCEPTION;

/**
 * Store some config files into the graph as "file" and "directory" elements.
 *
 */
public class ParseComponentConfFilesVerticle extends AbstractParseFileTask {
    protected Logger LOGGER = LoggerFactory.getLogger(ParseComponentConfFilesVerticle.class);

    @Resource
    Provider<YamlParser> yamlParserProvider;

    protected String[] getFilePatterns() {
        return new String[]{
            "Dockerfile",
            "**/package.json",
            "**/settings.gradle",
            "**/settings.gradle.kts",
            "**/gradle.properties",
            "**/build.gradle",
            "**/pom.xml",
            "**/go.mod",
            "**/src/main/resources/application.y*",
            "**/src/main/resources/application.json",
            "inventory.json",
        };
    }

    protected boolean isSpringYamlFile(String fileName) {
        String tail = Path.of(fileName).getFileName().toString();
        return "application.yml".equals(tail) || "application.yaml".equals(tail)
//            || "bootstrap.yaml".equals(fileName)
            ;
    }


    @Override
    protected void parseSingleFile(JsonObject domain, JsonObject component, String fileName)
            throws IOException
    {
        File parsedFile = new File(fileName);
        if (!parsedFile.isFile())
            return;
        synchronized (component) {
            String relativePath = FSUtils.relativePath(component, fileName);
            File file = new File(relativePath);
            Path filePath = file.toPath();
            String path = "";
            JsonObject sourceVertex = component;

            for (int i = 0; i < filePath.getNameCount() - 1; i++) {
                String name = filePath.getName(i).toString();
                path = path + "/" + name;
                sourceVertex = getOrCreateDirectoryVertex(component, sourceVertex, name, path, fileName);
            }

            String name = filePath.getName(filePath.getNameCount() - 1).toString();
            path = path + "/" + name;
            createVertexNode(component, sourceVertex, name, path, fileName, "file");
        }
    }

    private JsonObject createVertexNode(JsonObject component, JsonObject sourceVertex, String name, String path,
                                        String fileName, String type) throws IOException {
        String fileLink = GitUtils.buildRepositoryLink(component, fileName.split(name)[0] + name, config());
        JsonObject vertex = new JsonObject();
        vertex.put("id", UUID.randomUUID());
        vertex.put("type", type);
        vertex.put("path", path.substring(1));
        vertex.put("fileLink", fileLink);
        vertex.put("name", name);

        JsonObject edge = new JsonObject().put("type", type);
        graph.addEdge(sourceVertex, vertex, edge);
        getLogger().debug("Config file component added. id: {}, type: {}, name: {}, fileLink: {}",
                vertex.getString("id"), type, name, fileLink);

        if (type.equals("file")) {
            storeFileContent(component, vertex, fileName);
        }

        return vertex;
    }

    private JsonObject getOrCreateDirectoryVertex(JsonObject component, JsonObject sourceVertex,
            String name, String path, String fileName)
            throws IOException
    {
        JsonObject successor = V(sourceVertex.getString("id")).out()
            .has("type", "directory")
            .has("name", name).next();

        if (successor != null) {
            sourceVertex = successor;
        } else {
            sourceVertex = createVertexNode(component, sourceVertex, name, path, fileName, "directory");
        }

        return sourceVertex;
    }

    private void storeFileContent(JsonObject component, JsonObject vertex, String fileName) throws IOException {
        String content = FSUtils.readFileAsIs(fileName);
        vertex.put("content", content);
        try {
            if (fileName.endsWith(".yaml") || fileName.endsWith(".yml")) {
                YamlParser yamlParser = yamlParserProvider.get();
                List<Object> structuredJson = yamlParser.parseYamlData(content, fileName);
                if (isSpringYamlFile(fileName)) {
                    yamlParser.fixSpringYamlModels(structuredJson);
                }
                vertex.put("structured", new JsonArray(structuredJson));
            } else if (fileName.endsWith(".json")) {
                Object data = Json.CODEC.fromString(content, Object.class);
                vertex.put("structured", data); // Either JsonObject or JsonArray
            }
        } catch (Exception e){
            report.addMessage(EXCEPTION, component,
                    "Exception was thrown while handling '" + fileName
                            + "': " + e.getMessage() + "\nStacktrace:\n" + ExceptionUtils.getStackTrace(e));
        }
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }
}
