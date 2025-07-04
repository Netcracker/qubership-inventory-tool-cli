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
import com.google.inject.Scopes;
import com.google.inject.Singleton;

import io.vertx.core.json.JsonObject;
import jakarta.inject.Provider;
import org.qubership.itool.modules.processor.GraphMerger;
import org.qubership.itool.modules.processor.MergerApi;
import org.qubership.itool.modules.processor.matchers.CompoundVertexMatcher;
import org.qubership.itool.modules.processor.matchers.DefaultMockFieldExtractor;
import org.qubership.itool.modules.processor.matchers.FileMatcher;
import org.qubership.itool.modules.processor.matchers.MatcherById;
import org.qubership.itool.modules.processor.matchers.MockFieldExtractor;
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
        // Main merger class
        bind(MergerApi.class).to(GraphMerger.class);

        // Helper classes
        bind(MockFieldExtractor.class).to(DefaultMockFieldExtractor.class).in(Scopes.SINGLETON);

        // Concrete classes
        bind(CreateTransitiveQueueDependenciesTask.class);
        bind(CreateTransitiveHttpDependenciesTask.class);
        bind(RecreateHttpDependenciesTask.class);
        bind(RecreateDomainsStructureTask.class);

        bind(TargetMocksMatcher.class);
        bind(SourceMocksMatcher.class);
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
     * @param createTransitiveQueueDependenciesTaskProvider Provider for CreateTransitiveQueueDependenciesTask
     * @param createTransitiveHttpDependenciesTaskProvider Provider for CreateTransitiveHttpDependenciesTask
     * @param recreateDomainsStructureTaskProvider Provider for RecreateDomainsStructureTask
     * @param recreateHttpDependenciesTaskProvider Provider for RecreateHttpDependenciesTask
     * @return A list of finalization tasks in execution order
     */
    @Provides
    @FinalizationTasks
    public List<GraphProcessorTask> provideFinalizationTasks(
            Provider<CreateTransitiveQueueDependenciesTask> createTransitiveQueueDependenciesTaskProvider,
            Provider<CreateTransitiveHttpDependenciesTask> createTransitiveHttpDependenciesTaskProvider,
            Provider<RecreateDomainsStructureTask> recreateDomainsStructureTaskProvider,
            Provider<RecreateHttpDependenciesTask> recreateHttpDependenciesTaskProvider) {
        return List.of(
            recreateHttpDependenciesTaskProvider.get(),
            createTransitiveQueueDependenciesTaskProvider.get(),
            createTransitiveHttpDependenciesTaskProvider.get(),
            recreateDomainsStructureTaskProvider.get()
        );
    }

    /**
     * Provides a compound vertex matcher with the default matchers in the correct order.
     * The order is important as matchers are tried in sequence.
     *
     * @param targetMocksMatcher The target mocks matcher
     * @param sourceMocksMatcher The source mocks matcher
     * @return CompoundVertexMatcher with all matchers in the correct order
     */
    @Provides
    public CompoundVertexMatcher provideCompoundVertexMatcher(TargetMocksMatcher targetMocksMatcher, SourceMocksMatcher sourceMocksMatcher) {
        return new CompoundVertexMatcher(
            new MatcherById(),  // Shall be the first in list
            targetMocksMatcher,
            sourceMocksMatcher,
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
