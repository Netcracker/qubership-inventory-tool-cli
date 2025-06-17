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

package org.qubership.itool.context;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.qubership.itool.modules.graph.Graph;
import org.qubership.itool.modules.graph.GraphService;

/**
 * Factory interface for creating FlowContext instances.
 * This allows for custom FlowContext implementations to be used in the system.
 */
public interface FlowContextFactory {
    
    /**
     * Creates a new FlowContextFactory instance.
     * The actual implementation class is determined by the application config property "flowContextFactory".
     * If not specified in config, DefaultFlowContextFactory is used.
     *
     * @param config Application configuration containing the "flowContextFactory" property
     * @return A new FlowContextFactory instance
     */
    static FlowContextFactory create(JsonObject config) {
        String factoryClass = config.getString("flowContextFactory", DefaultFlowContextFactory.class.getName());
        try {
            return (FlowContextFactory) Class.forName(factoryClass).getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create FlowContextFactory: " + factoryClass, e);
        }
    }

    /**
     * Creates a new FlowContext instance with default configuration.
     * @return A new FlowContext instance
     */
    FlowContext createFlowContext();

    /**
     * Creates a new FlowContext instance with a specific Graph.
     * @param graph The graph to use in the context
     * @return A new FlowContext instance
     */
    FlowContext createFlowContext(Graph graph);

    /**
     * Creates a new FlowContext instance with a specific GraphService.
     * @param graphService The graph service to use in the context
     * @return A new FlowContext instance
     */
    FlowContext createFlowContext(GraphService graphService);

    /**
     * Initializes a FlowContext with Vertx and configuration.
     * @param context The context to initialize
     * @param vertx The Vertx instance to use
     * @param config The configuration to use
     */
    void initializeFlowContext(FlowContext context, Vertx vertx, JsonObject config);
} 