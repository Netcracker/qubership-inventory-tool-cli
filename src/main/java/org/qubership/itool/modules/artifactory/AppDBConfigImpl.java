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

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;
import org.qubership.itool.utils.ConfigProperties;

public class AppDBConfigImpl implements AppDBConfig {

    protected String url;
    protected String authMethod;
    protected String user;
    protected String password;
    protected String mavenEntry;

    /* A constructor for default auth method (Basic) */
    public AppDBConfigImpl(String url, String user, String password, String mavenEntry) {
        this.url = url;
        this.authMethod = BASIC_AUTH;
        this.user = user;
        this.password = password;
        this.mavenEntry = mavenEntry;
    }

    /* A constructor from JsonObject config */
    public AppDBConfigImpl(JsonObject config) {
        this.url = (String) JsonPointer.from(ConfigProperties.APPDB_URL_POINTER).queryJson(config);
        this.authMethod = (String) JsonPointer.from(ConfigProperties.APPDB_AUTH_METHOD_POINTER)
                .queryJsonOrDefault(config, BASIC_AUTH);
        switch (authMethod.toLowerCase()) {
        case BASIC_AUTH:
            this.user = config.getString("login");
            this.password = config.getString("password");
            break;
        default:
            throw new IllegalArgumentException(ConfigProperties.APPDB_AUTH_METHOD_POINTER + "=" + authMethod + " not supported");
        }
        this.mavenEntry = (String) JsonPointer.from(ConfigProperties.APPDB_MAVEN_ENTRY_POINTER)
                .queryJsonOrDefault(config, DEFAULT_MAVEN_ENTRY);
    }

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public String getAuthMethod() {
        return authMethod;
    }

    @Override
    public String getUser() {
        return user;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getMavenEntry() {
        return mavenEntry;
    }
}
