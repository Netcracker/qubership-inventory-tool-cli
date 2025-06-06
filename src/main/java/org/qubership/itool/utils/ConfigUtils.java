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

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.qubership.itool.modules.graph.GraphDataConstants;

import static org.qubership.itool.utils.ConfigProperties.*;

public class ConfigUtils {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigUtils.class);

    public static String getConfigValue(String jsonPointer, JsonObject config) {
        Object value = JsonPointer.from(jsonPointer).queryJson(config);
        if (value == null) {
            return null;
        }
        return value.toString();
    }

    public static boolean isFeatureEnabled(String feature, JsonObject config) {
        String value = getConfigValue(DISABLED_FEATURES_POINTER, config);
        if (value == null) {
            return true;
        }
        return !value.contains(feature);
    }

    public static Future<List<Path>> getFilesFromJsonConfig(Vertx vertx, JsonObject config, JsonPointer listPointer, String... path) {
        return vertx.fileSystem().readFile(ConfigUtils.getConfigFilePath(config, path).toString())
                .map(fileContents -> {
                    JsonObject json = new JsonObject(fileContents);
                    List<Path> filesArray = ((JsonArray) listPointer.queryJson(json)).stream()
                            .map(str -> Path.of((String) str))
                            .collect(Collectors.toList());
                    return filesArray;
                });
    }

    public static Path getConfigFilePath(JsonObject config, String... path) {
        Path configPath = Path.of(getConfigValue(CONFIG_PATH_POINTER, config),
                RELEASES_DIR, getConfigValue(RELEASE_POINTER, config));
        Path configFilePath = Path.of(configPath.toString(), path);

        if (!configFilePath.toFile().exists()) {
            configPath = Path.of(getConfigValue(CONFIG_PATH_POINTER, config), DEFAULT_RELEASE);
            configFilePath = Path.of(configPath.toString(), path);
        }
        return configFilePath;
    }

    public static Path getSuperRepoFilePath(JsonObject config, String... path) {
        return Path.of(ConfigUtils.getConfigValue(SUPER_REPOSITORY_DIR_POINTER, config), path);
    }

    private static volatile Properties buildProperties;

    public static Properties getInventoryToolBuildProperties() {
        if (buildProperties != null) {
            return buildProperties;
        }

        Properties buildProps = new Properties();
        try (InputStream is = FSUtils.openUrlStream(ConfigUtils.class, "classpath:/inventory.tool.build.properties")) {
            // Update buffer size if that file grows!
            BufferedInputStream buffer = new BufferedInputStream(is, 256);
            buildProps.load(buffer);
            buildProperties = buildProps;
        } catch (IOException e) {
            LOG.error("Failed to read /inventory.tool.build.properties", e);
        }
        return buildProps;
    }

    //-- Add common prefix to regular domains

    public static String fillDomainId(String domainId) {
        return (GraphDataConstants.UNKNOWN_DOMAIN_NAME.equals(domainId) || domainId.startsWith(GraphDataConstants.DOMAIN_ID_PREFIX))
                ? domainId
                : GraphDataConstants.DOMAIN_ID_PREFIX + domainId;
    }

    public static String stripDomainId(String domainId) {
        return domainId.startsWith(GraphDataConstants.DOMAIN_ID_PREFIX)
                ? domainId.substring(GraphDataConstants.DOMAIN_ID_PREFIX.length())
                : domainId;
    }

}
