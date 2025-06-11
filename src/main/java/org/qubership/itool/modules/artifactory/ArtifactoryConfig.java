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

package org.qubership.itool.modules.artifactory;

import org.qubership.itool.utils.ConfigProperties;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;

public class ArtifactoryConfig {

    protected final String server;
    protected final String storage;

    public ArtifactoryConfig(String server, String storage) {
        this.server = server;
        this.storage = storage;
    }

    public ArtifactoryConfig(JsonObject config) {
        this.server = (String) JsonPointer.from(ConfigProperties.ARTIFACTORY_SERVER_POINTER).queryJson(config);
        this.storage = (String) JsonPointer.from(ConfigProperties.ARTIFACTORY_STORAGE_POINTER).queryJson(config);
    }

    public String getServer() {
        return server;
    }

    public String getStorage() {
        return storage;
    }

    @Override
    public String toString() {
        return "ArtifactoryConfig [server=" + server + ", storage=" + storage + "]";
    }

}
