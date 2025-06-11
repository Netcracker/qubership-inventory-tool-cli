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

package org.qubership.itool.modules.git;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.qubership.itool.modules.report.GraphReport;
import org.qubership.itool.utils.ConfigProperties;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class GitFileRetrieverBuilder {

    protected static final Logger LOG = LoggerFactory.getLogger(GitFileRetrieverBuilder.class);

    @Nullable
    public static GitFileRetriever create(GitAdapter gitAdapter, JsonObject config, Vertx vertx, GraphReport report)
    {
        boolean offlineMode = gitAdapter==null || Boolean.parseBoolean(config.getString(ConfigProperties.OFFLINE_MODE));
        if (offlineMode) {
            LOG.warn("Offline mode, GIT facilities will not be available");
            return null;
        }
        return new GitFileRetrieverImpl(gitAdapter, config, vertx, report);
    }

}
