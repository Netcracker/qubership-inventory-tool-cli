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
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import com.google.inject.util.Modules;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import jakarta.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.qubership.itool.modules.graph.Graph;
import org.qubership.itool.modules.processor.tasks.GraphProcessorTask;
import org.qubership.itool.modules.processor.tasks.PatchIsMicroserviceFieldTask;
import org.qubership.itool.modules.processor.tasks.PatchMockedComponentsNormalizationTask;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test case demonstrating how extension projects can modify or extend task lists.
 * This shows the extensibility of the current @Provides approach.
 */
@ExtendWith(VertxExtension.class)
class TaskListExtensionTest {

    private Vertx vertx;

    @BeforeEach
    void setUp(VertxTestContext testContext) {
        vertx = Vertx.vertx();
        testContext.completeNow();
    }

    /**
     * Test demonstrating how an extension can add additional tasks to the normalization list.
     */
    @Test
    void testExtendNormalizationTasks(VertxTestContext testContext) {
        // Create an extension module that adds a custom task
        Module extensionModule = new AbstractModule() {
            @Override
            protected void configure() {
                // Extension can bind additional tasks
                bind(CustomNormalizationTask.class);
            }

            @Provides
            @NormalizationTasks
            public List<GraphProcessorTask> provideExtendedNormalizationTasks(
                    Provider<PatchIsMicroserviceFieldTask> patchIsMicroserviceFieldTaskProvider,
                    Provider<PatchMockedComponentsNormalizationTask> patchMockedComponentsNormalizationTaskProvider,
                    Provider<CustomNormalizationTask> customNormalizationTaskProvider) {

                // Add our custom task at the end
                return List.of(
                    patchIsMicroserviceFieldTaskProvider.get(),
                    patchMockedComponentsNormalizationTaskProvider.get(),
                    customNormalizationTaskProvider.get()  // Our custom task
                );
            }
        };

        // Create application context with extension module
        Module baseModule = new QubershipModule(vertx);
        Module combined = Modules.override(baseModule).with(extensionModule);

        ApplicationContext context = new ApplicationContext(vertx, new JsonObject(), new Module[] {combined});

        // Get the normalization tasks using the injector directly
        List<GraphProcessorTask> normalizationTasks = context.getInjector().getInstance(
            Key.get(new TypeLiteral<List<GraphProcessorTask>>() {}, NormalizationTasks.class)
        );

        // Verify we have the expected tasks in the correct order
        assertEquals(3, normalizationTasks.size(), "Should have 3 normalization tasks");
        assertInstanceOf(PatchIsMicroserviceFieldTask.class, normalizationTasks.get(0),
            "First task should be PatchIsMicroserviceFieldTask");
        assertInstanceOf(PatchMockedComponentsNormalizationTask.class, normalizationTasks.get(1),
            "Second task should be PatchMockedComponentsNormalizationTask");
        assertInstanceOf(CustomNormalizationTask.class, normalizationTasks.get(2),
            "Third task should be our CustomNormalizationTask");

        testContext.completeNow();
    }

    // Custom task implementations for testing
    private static class CustomNormalizationTask implements GraphProcessorTask {
        @Override
        public void process(Graph graph) {
            // Custom normalization logic
        }
    }
}
