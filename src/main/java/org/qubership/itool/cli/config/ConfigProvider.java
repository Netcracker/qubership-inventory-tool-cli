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

package org.qubership.itool.cli.config;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.*;
import io.vertx.core.impl.future.SucceededFuture;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;

import org.apache.commons.lang3.StringUtils;
import org.qubership.itool.utils.ConfigProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static org.qubership.itool.utils.ConfigProperties.*;
import static org.qubership.itool.utils.ConfigUtils.getConfigValue;

public class ConfigProvider {

    protected static final Logger LOG = LoggerFactory.getLogger(ConfigProvider.class);

    public static final List<String> HIDDEN_PROPERTIES = List.of(
            PASSWORD_PROPERTY
    );


    private Map<String, String> customProperties;
    private Vertx vertx;

    public ConfigProvider(Map<String, String> customProperties, Vertx vertx) {
        this.customProperties = customProperties;
        this.vertx = vertx;
    }


    public void handleConfig(Handler<AsyncResult<JsonObject>> asyncResultHandler) {
        ConfigRetriever retriever = retrieveConfig(customProperties, vertx);
        retriever.getConfig()
                .onComplete(ar -> fillPassword(ar.result()))
                .onComplete(asyncResultHandler);
    }

    public Future<JsonObject> fillPassword(JsonObject config) {
        if (StringUtils.isNotEmpty(config.getString(PASSWORD_PROPERTY))) {
            return new SucceededFuture<>(config);
        }

        if (Boolean.parseBoolean(config.getString(OFFLINE_MODE_PROPERTY))) {
            // Assume we do not need to read passwords from anywhere and use them in offline mode
            LOG.warn("Offline mode, got no password");
            return new SucceededFuture<>(config);
        }

        String passwordUrl = config.getString(PASSWORD_SOURCE_PROPERTY);
        if (StringUtils.isNotEmpty(passwordUrl) && ! "null".equals(passwordUrl)) {
            String passwordStr = readPasswordFromUrl(passwordUrl);
            if (passwordStr != null) {
                config.put(PASSWORD_PROPERTY, passwordStr);
                return new SucceededFuture<>(config);
            }
        }

        WorkerExecutor executor = vertx.createSharedWorkerExecutor("console-password-asker", 1, 15, TimeUnit.MINUTES);
        return executor.executeBlocking(() -> {
            String passwordStr = readPasswordFromConsole();
            config.put(PASSWORD_PROPERTY, passwordStr);
            return config;
        });
    }

    private String readPasswordFromUrl(String passwordUrl) {
        try {
            // Consider it fast enough. Otherwise, we will need the same technique as in readPasswordFromConsole()
            URL url = new URL(passwordUrl);
            try (InputStream s = url.openStream()) {
                return new LineNumberReader(new InputStreamReader(s)).readLine();
            }
        } catch (FileNotFoundException e) {
            LOG.warn("Exception when reading the password from a file: " + e.getMessage());
            return null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String readPasswordFromConsole() {
        try {
            Console console = System.console();
            if (console != null) {
                return new String(console.readPassword("Password: "));
            } else {
                System.out.print("Password: ");
                InputStream in = System.in;
                int max=50;
                byte[] b = new byte[max];

                int l = in.read(b);
                while (l > 0 && (b[l-1]=='\r' || b[l-1]=='\n')) {
                    l--;
                }
                if (l > 0) {
                    return new String(b, 0, l);
                } else {
                    return null;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static ConfigRetriever retrieveConfig(Map<String, String> customProperties, Vertx vertx) {
        JsonObject config = new JsonObject();
        Pattern booleanPattern = Pattern.compile("true|false", Pattern.CASE_INSENSITIVE);
        for (Map.Entry<String, String> e: customProperties.entrySet()) {
            String key = e.getKey();
            String value = e.getValue();
            if (key.startsWith("/")) {
                JsonPointer pointer = JsonPointer.from(key);
                if (booleanPattern.matcher(value).matches()) {
                    pointer.writeJson(config, Boolean.valueOf(value), true);
                } else {
                    pointer.writeJson(config, StringUtils.trimToEmpty(value), true);
                }
            } else {
                config.put(key, StringUtils.trimToEmpty(value));
            }
        }

        ConfigStoreOptions defaultProfile = new ConfigStoreOptions()
                .setType("file")
                .setFormat("properties")
                .setConfig(new JsonObject()
                        .put("path", Path.of(getConfigValue(CONFIG_PATH_POINTER, config),
                                "default", "config", "profiles", "default.properties").toString())
                        .put("hierarchical", true)
                );

        // Supports properties and json format.
        // To support yaml or other formats add according extensions to dependencies
        String profile = getConfigValue(PROFILE_POINTER, config);
        String profileType = "properties";
        if (profile == null) {
            profile = "";
        }
        if (profile.contains(".")) {
            profileType = profile.split("\\.")[1];
        } else {
            profile = profile + "." + profileType;
        }

        ConfigStoreOptions customProfile = new ConfigStoreOptions()
                .setType("file")
                .setFormat(profileType)
                .setOptional(true)
                .setConfig(new JsonObject()
                        .put("path", Path.of(getConfigValue(CONFIG_PATH_POINTER, config),
                                "default", "config", "profiles", profile).toString())
                        .put("hierarchical", true)
                );
        ConfigStoreOptions argsJson = new ConfigStoreOptions()
                .setType("json")
                .setConfig(config);

        ConfigRetriever retriever = ConfigRetriever.create(vertx, new ConfigRetrieverOptions()
                .addStore(defaultProfile)
                .addStore(customProfile)
                .addStore(argsJson)
        );

        return retriever;
    }

}
