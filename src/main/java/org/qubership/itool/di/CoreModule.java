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
import jakarta.inject.Provider;

import org.qubership.itool.modules.report.GraphReport;
import org.qubership.itool.modules.report.GraphReportImpl;
import org.qubership.itool.modules.template.TemplateService;
import org.qubership.itool.modules.template.TemplateServiceImpl;

import java.util.Properties;

import org.qubership.itool.modules.diagram.DiagramService;
import org.qubership.itool.modules.diagram.DiagramServiceImpl;
import org.qubership.itool.modules.diagram.providers.DomainDiagramProvider;
import org.qubership.itool.modules.diagram.providers.GeneralDomainsDiagramProvider;
import org.qubership.itool.modules.diagram.providers.InfrastructureDiagramProvider;
import org.qubership.itool.modules.diagram.providers.MicroserviceDiagramProvider;
import org.qubership.itool.modules.diagram.providers.QueueDiagramProvider;
import org.qubership.itool.modules.graph.Graph;
import org.qubership.itool.modules.graph.GraphImpl;

import static org.qubership.itool.modules.diagram.providers.DiagramProvider.SKINPARAM_BACKGROUND_COLOR_CACHING;
import static org.qubership.itool.modules.diagram.providers.DiagramProvider.SKINPARAM_BACKGROUND_COLOR_DATABASE;
import static org.qubership.itool.modules.diagram.providers.DiagramProvider.SKINPARAM_BACKGROUND_COLOR_DEFAULT_COMPONENT;
import static org.qubership.itool.modules.diagram.providers.DiagramProvider.SKINPARAM_BACKGROUND_COLOR_DEFAULT_DOMAIN;
import static org.qubership.itool.modules.diagram.providers.DiagramProvider.SKINPARAM_BACKGROUND_COLOR_QUEUE;

/**
 * Core module that provides basic bindings for the core functionality.
 * This module can be extended or overridden by users of the library.
 */
public class CoreModule extends AbstractModule {

    private final Vertx vertx;

    public CoreModule(Vertx vertx) {
        this.vertx = vertx;
    }

    @Override
    protected void configure() {
        // Bind core factories
        bind(GraphReport.class).to(GraphReportImpl.class).asEagerSingleton();
        bind(Vertx.class).toInstance(vertx);
    }

    @Provides
    @Singleton
    public Graph provideGraph(Provider<GraphReport> graphReportProvider) {
        Graph graph = new GraphImpl();
        graph.setReport(graphReportProvider.get());
        return graph;
    }

    /**
     * Provides diagram properties with default color configurations.
     *
     * @return Properties object with diagram skin parameters
     */
    @Provides
    @Singleton
    @Named("diagram.properties")
    public Properties provideDiagramProperties() {
        Properties diagramProperties = new Properties();
        diagramProperties.setProperty(SKINPARAM_BACKGROUND_COLOR_DEFAULT_DOMAIN, "Gold");
        diagramProperties.setProperty(SKINPARAM_BACKGROUND_COLOR_DEFAULT_COMPONENT, "Yellow");
        diagramProperties.setProperty(SKINPARAM_BACKGROUND_COLOR_DATABASE, "DeepSkyBlue");
        diagramProperties.setProperty(SKINPARAM_BACKGROUND_COLOR_QUEUE, "GreenYellow");
        diagramProperties.setProperty(SKINPARAM_BACKGROUND_COLOR_CACHING, "Orchid");
        return diagramProperties;
    }

    /**
     * Provides DiagramService with all diagram providers registered.
     *
     * @param graphProvider     Provider for Graph instance
     * @param diagramProperties Diagram properties with skin parameters
     * @return Configured DiagramService instance
     */
    @Provides
    @Singleton
    public DiagramService provideDiagramService(Provider<Graph> graphProvider, @Named("diagram.properties") Properties diagramProperties) {
        DiagramService diagramService = new DiagramServiceImpl(graphProvider.get(), diagramProperties);

        // Register all diagram providers
        diagramService.register(new MicroserviceDiagramProvider());
        diagramService.register(new DomainDiagramProvider());
        diagramService.register(new GeneralDomainsDiagramProvider());
        diagramService.register(new InfrastructureDiagramProvider());
        diagramService.register(new QueueDiagramProvider());

        return diagramService;
    }

    /**
     * Provides TemplateService with DiagramService dependency.
     *
     * @param diagramService DiagramService instance
     * @param config         Application configuration
     * @return Configured TemplateService instance
     */
    @Provides
    @Singleton
    public TemplateService provideTemplateService(DiagramService diagramService,
            @Named("application.config") JsonObject config) {
        return new TemplateServiceImpl(diagramService, config);
    }
}
