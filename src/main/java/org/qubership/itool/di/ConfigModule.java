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
package org.qubership.itool.di;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import io.vertx.core.json.JsonObject;

/**
 * Module that provides application configuration.
 * This module binds the application configuration JsonObject with a named binding
 * to distinguish it from other JsonObject instances that might be needed in the future.
 */
public class ConfigModule extends AbstractModule {

    private final JsonObject config;

    /**
     * Creates a new ConfigModule with the application configuration.
     *
     * @param config The application configuration
     */
    public ConfigModule(JsonObject config) {
        this.config = config;
    }

    @Override
    protected void configure() {
        bind(JsonObject.class)
            .annotatedWith(Names.named("application.config"))
            .toInstance(config);
    }
}
