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
import com.google.inject.Module;
import com.google.inject.util.Modules;
import io.vertx.core.Vertx;
import jakarta.inject.Provider;

import org.qubership.itool.context.FlowContext;
import org.qubership.itool.modules.processor.GraphMerger;
import org.qubership.itool.modules.report.GraphReport;

/**
 * Test module that demonstrates how to override core services with test implementations.
 * Uses Guice's override feature to replace existing bindings.
 */
public class TestModule extends AbstractModule {
    private final GraphMerger mockGraphMerger;
    private final FlowContext mockFlowContext;
    private final GraphReport customGraphReport;

    public TestModule(GraphMerger mockGraphMerger,
                     FlowContext mockFlowContext,
                     GraphReport customGraphReport) {
        this.mockGraphMerger = mockGraphMerger;
        this.mockFlowContext = mockFlowContext;
        this.customGraphReport = customGraphReport;
    }

    @Override
    protected void configure() {
        // Override existing bindings with mocks and custom implementations
        bind(GraphMerger.class).toInstance(mockGraphMerger);
        bind(FlowContext.class).toInstance(mockFlowContext);
        bind(GraphReport.class).toInstance(customGraphReport);
    }

    /**
     * Create a module that overrides the base CLI module with test implementations.
     * 
     * @param vertx The Vertx instance
     * @param mockGraphMerger The mock GraphMerger
     * @param mockFlowContext The mock FlowContext
     * @param customGraphReport Custom GraphReport implementation
     * @return A module that can be used with ApplicationContext
     */
    public static Module createOverrideModule(Vertx vertx, 
                                            GraphMerger mockGraphMerger,
                                            FlowContext mockFlowContext,
                                            GraphReport customGraphReport) {
        return Modules.override(new QubershipModule(vertx))
                     .with(new TestModule(mockGraphMerger, mockFlowContext, customGraphReport));
    }
} 