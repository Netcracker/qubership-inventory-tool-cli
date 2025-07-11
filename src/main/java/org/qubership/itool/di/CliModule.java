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
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;

import org.qubership.itool.context.FlowContext;
import org.qubership.itool.context.FlowContextImpl;
import org.qubership.itool.modules.confluence.ConfluenceClient;
import org.qubership.itool.modules.confluence.ConfluenceClientBuilder;
import org.qubership.itool.modules.git.GitAdapter;
import org.qubership.itool.modules.git.GitAdapterBuilder;
import org.qubership.itool.modules.git.GitFileRetriever;
import org.qubership.itool.modules.git.GitFileRetrieverBuilder;
import org.qubership.itool.modules.graph.GraphClassifierBuilder;
import org.qubership.itool.modules.graph.GraphClassifierBuilderImpl;
import org.qubership.itool.modules.parsing.InventoryJsonParser;
import org.qubership.itool.modules.report.GraphReport;
import org.qubership.itool.modules.template.ConfluencePage;
import org.qubership.itool.utils.YamlParser;

/**
 * CLI-specific module that extends the base module with CLI-specific bindings.
 * Can be extended or overridden by extension applications.
 */
public class CliModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(FlowContext.class).to(FlowContextImpl.class).in(Singleton.class);
        bind(YamlParser.class).in(Singleton.class);
        bind(InventoryJsonParser.class).in(Singleton.class);
        bind(ConfluencePage.class);
        bind(GraphClassifierBuilder.class).to(GraphClassifierBuilderImpl.class);
    }

    /**
     * Provides WebClient instance.
     *
     * @param vertx The Vertx instance
     * @return WebClient instance
     */
    @Provides
    @Singleton
    public WebClient provideWebClient(Vertx vertx) {
        return WebClient.create(vertx);
    }

    /**
     * Provides GitAdapter instance.
     *
     * @param vertx The Vertx instance
     * @param report The GraphReport instance
     * @param config The application configuration
     * @return GitAdapter instance or null if offline mode
     */
    @Provides
    public GitAdapter provideGitAdapter(Vertx vertx, GraphReport report, @Named("application.config") JsonObject config) {
        return GitAdapterBuilder.create(vertx, report, config);
    }

    /**
     * Provides ConfluenceClient instance.
     *
     * @param vertx The Vertx instance
     * @param webClient The WebClient instance
     * @param config The application configuration
     * @return ConfluenceClient instance or null if offline mode
     */
    @Provides
    public ConfluenceClient provideConfluenceClient(Vertx vertx, WebClient webClient, @Named("application.config") JsonObject config) {
        return ConfluenceClientBuilder.create(vertx, webClient, config);
    }

    /**
     * Provides GitFileRetriever instance.
     *
     * @param gitAdapter The GitAdapter instance
     * @param config The application configuration
     * @param vertx The Vertx instance
     * @param report The GraphReport instance
     * @return GitFileRetriever instance or null if offline mode
     */
    @Provides
    public GitFileRetriever provideGitFileRetriever(GitAdapter gitAdapter, @Named("application.config") JsonObject config, Vertx vertx, GraphReport report) {
        return GitFileRetrieverBuilder.create(gitAdapter, config, vertx, report);
    }
}
