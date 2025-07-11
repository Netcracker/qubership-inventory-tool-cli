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

import io.vertx.core.json.JsonObject;
import jakarta.inject.Provider;
import org.qubership.itool.modules.processor.GraphMerger;
import org.qubership.itool.modules.processor.MergerApi;
import org.qubership.itool.modules.processor.matchers.CompoundVertexMatcher;
import org.qubership.itool.modules.processor.matchers.FileMatcher;
import org.qubership.itool.modules.processor.matchers.MatcherById;
import org.qubership.itool.modules.processor.matchers.SourceMocksMatcher;
import org.qubership.itool.modules.processor.matchers.TargetMocksMatcher;
import org.qubership.itool.modules.processor.tasks.GraphProcessorTask;
import org.qubership.itool.modules.processor.tasks.PatchAppVertexTask;
import org.qubership.itool.modules.processor.tasks.PatchIsMicroserviceFieldTask;
import org.qubership.itool.modules.processor.tasks.PatchMockedComponentsNormalizationTask;
import org.qubership.itool.modules.processor.tasks.PatchVertexDnsNamesNormalizationTask;
import org.qubership.itool.modules.processor.tasks.PatchLanguagesNormalizationTask;
import org.qubership.itool.modules.processor.tasks.CreateAppVertexTask;
import org.qubership.itool.modules.processor.tasks.RecreateHttpDependenciesTask;
import org.qubership.itool.modules.processor.tasks.CreateTransitiveQueueDependenciesTask;
import org.qubership.itool.modules.processor.tasks.CreateTransitiveHttpDependenciesTask;
import org.qubership.itool.modules.processor.tasks.RecreateDomainsStructureTask;
import org.qubership.itool.modules.report.GraphReport;

import java.util.List;
import java.util.function.Function;

/**
 * Module for merger-related bindings including normalization and finalization tasks.
 * This module provides the GraphMerger and its associated tasks that are applied
 * during graph merging.
 */
public class MergerModule extends AbstractModule {

    @Override
    protected void configure() {
        // Bind the interface to implementation
        bind(MergerApi.class).to(GraphMerger.class);
    }

    /**
     * Provides a list of normalization tasks in the order they should be executed.
     * The order is important as some tasks may depend on the results of previous tasks.
     *
     * @return A list of normalization tasks in execution order
     */
    @Provides
    @NormalizationTasks
    public List<GraphProcessorTask> provideNormalizationTasksList() {
        return List.of(
            new PatchIsMicroserviceFieldTask(),
            new PatchMockedComponentsNormalizationTask(),
            new PatchVertexDnsNamesNormalizationTask(),
            new PatchLanguagesNormalizationTask()
        );
    }

    /**
     * Provides a list of finalization tasks in the order they should be executed.
     * The order is important as some tasks may depend on the results of previous tasks.
     *
     * @param graphReportProvider Provider for GraphReport
     * @return A list of finalization tasks in execution order
     */
    @Provides
    @FinalizationTasks
    public List<GraphProcessorTask> provideFinalizationTasks(
            Provider<GraphReport> graphReportProvider) {
        return List.of(
            new RecreateHttpDependenciesTask(),
            new CreateTransitiveQueueDependenciesTask(),
            new CreateTransitiveHttpDependenciesTask(),
            new RecreateDomainsStructureTask(graphReportProvider)
        );
    }

    /**
     * Provides a compound vertex matcher with the default matchers in the correct order.
     * The order is important as matchers are tried in sequence.
     *
     * @return CompoundVertexMatcher with all matchers in the correct order
     */
    @Provides
    public CompoundVertexMatcher provideCompoundVertexMatcher() {
        return new CompoundVertexMatcher(
            new MatcherById(),  // Shall be the first in list
            new TargetMocksMatcher(),
            new SourceMocksMatcher(),
            new FileMatcher()
        );
    }

    @Provides
    @Singleton
    public Function<JsonObject, CreateAppVertexTask> provideCreateAppVertexTaskFactory() {
        return CreateAppVertexTask::new;
    }

    @Provides
    @Singleton
    public Function<JsonObject, PatchAppVertexTask> providePatchAppVertexTaskFactory() {
        return PatchAppVertexTask::new;
    }
}
