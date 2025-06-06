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

package org.qubership.itool.modules.confluence;

import org.qubership.itool.utils.ConfigProperties;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

public class ConfluenceClientBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(ConfluenceClientBuilder.class);

    @Nullable
    public static ConfluenceClient create(Vertx vertx, WebClient client, JsonObject config) {
        boolean offlineMode = Boolean.parseBoolean(config.getString(ConfigProperties.OFFLINE_MODE));
        if (offlineMode) {
            LOG.warn("Offline mode, Confluence facilities will not be available");
            return null;
        }

        String confluenceUrl = config.getString("confluenceUrl");
        if (confluenceUrl == null) {
            LOG.warn("No URL to confluence provided, Confluence facilities will not be available");
            return null;
        }
        String password = config.getString("password");
        String login = config.getString("login");
        if (login==null || password==null) {
            LOG.warn("No login or password provided, Confluence facilities will not be available");
            return null;
        }

        return new ConfluenceClientImpl(vertx, client, config);
    }

}
