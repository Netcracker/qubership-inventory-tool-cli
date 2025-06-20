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
import io.vertx.core.Vertx;
import jakarta.inject.Provider;

import org.qubership.itool.modules.report.GraphReport;
import org.qubership.itool.modules.report.GraphReportImpl;
import org.qubership.itool.modules.graph.Graph;
import org.qubership.itool.modules.graph.GraphImpl;

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
        bind(GraphReport.class).to(GraphReportImpl.class);
        bind(Vertx.class).toInstance(vertx);
    }

    @Provides
    public Graph provideGraph(Provider<GraphReport> graphReportProvider) {
        Graph graph = new GraphImpl();
        graph.setReport(graphReportProvider.get());
        return graph;
    }
} 