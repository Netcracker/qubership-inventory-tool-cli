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

package org.qubership.itool.cli;

import com.google.inject.Module;
import com.google.inject.util.Modules;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.cli.annotations.Description;
import io.vertx.core.cli.annotations.Option;
import io.vertx.core.impl.launcher.commands.ClasspathHandler;
import io.vertx.core.json.JsonObject;
import io.vertx.core.spi.VerticleFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.qubership.itool.modules.graph.GraphService;
import org.slf4j.Logger;

import org.qubership.itool.cli.config.ConfigProvider;
import org.qubership.itool.context.FlowContext;
import org.qubership.itool.di.ApplicationContext;
import org.qubership.itool.di.QubershipModule;
import org.qubership.itool.factories.JavaAppContextVerticleFactory;

import static org.qubership.itool.utils.ConfigProperties.CONFIG_PATH_POINTER;
import static org.qubership.itool.utils.ConfigProperties.PROFILE_POINTER;


/**
 * Abstract base class for all CLI commands in the inventory tool.
 * Provides common functionality for command execution, configuration handling,
 * and flow management.
 */
public abstract class AbstractCommand extends ClasspathHandler {

    /**
     * Configuration properties map containing default values for the command.
     */
    protected Map<String, String> properties = new HashMap<>(Map.of(
            PROFILE_POINTER, "dev",
            CONFIG_PATH_POINTER, "inventory-tool"
    ));

    /**
     * Returns the logger instance for this command.
     *
     * @return the logger instance
     */
    protected abstract Logger getLogger();

    /** Create VertX, start a flow using {@link #properties} and config, let the flow complete, terminate JVM
     *
     * @param main Main verticle
     * @param graphService Graph Service to work with. May be omitted (set to null)
     */
    public void runFlow(FlowMainVerticle main, GraphService graphService) {
        Vertx vertx = Vertx.vertx();
        vertx.exceptionHandler(err -> {
            getLogger().error("Critical error, application is stopping", err);
            System.exit(1);
        });

        runFlow(vertx, main, graphService)
            .onComplete(ar -> {
                if (ar.failed()) {
                    getLogger().error("Flow execution failed", ar.cause());
                    System.exit(1);
                }
                System.exit(0);
            });
    }

    /**
     * Runs the flow with the given Vertx instance, main verticle, and graph service.
     *
     * @param vertx the Vertx instance
     * @param main the main verticle
     * @param graphService the graph service (may be null)
     * @return a future that completes when the flow finishes
     */
    protected Future<?> runFlow(Vertx vertx, FlowMainVerticle main, GraphService graphService) {
        ConfigProvider configProvider = new ConfigProvider(properties, vertx);
        return Future.future(promise ->
            configProvider.handleConfig(ar -> configLoaded(vertx, main, graphService, ar.result(), promise)));
    }

    /**
     * Handles the configuration after it has been loaded.
     *
     * @param vertx the Vertx instance
     * @param main the main verticle
     * @param graphService the graph service
     * @param config the loaded configuration
     * @param promise the promise to complete
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected void configLoaded(Vertx vertx, FlowMainVerticle main, GraphService graphService, JsonObject config, Promise promise) {
        try {
            // Create application context with the loaded config and custom modules
            ApplicationContext context = new ApplicationContext(vertx, config, createModules(vertx));

            // Get flow context from the application context
            FlowContext flowContext = context.getInstance(FlowContext.class);

            // XXX Still needs reviewing for real multi-flow design
            Optional<VerticleFactory> factory = vertx.verticleFactories()
                    .stream()
                    .filter(f -> f instanceof JavaAppContextVerticleFactory)
                    .findAny();
            JavaAppContextVerticleFactory javaTaskFactory;
            if (factory.isEmpty()) {
                javaTaskFactory = new JavaAppContextVerticleFactory(flowContext, config);
                vertx.registerVerticleFactory(javaTaskFactory);
            } else {
                javaTaskFactory = (JavaAppContextVerticleFactory) factory.get();
            }

            flowContext.initialize(vertx, config);
            flowContext.setTaskClassLoader(javaTaskFactory.getTaskClassLoader());

            main.deployAndRunFlow(flowContext)
                .onComplete(flowResult -> {
                    promise.handle(flowResult);
                    flowFinished(main, flowResult);
                });
        } catch (Throwable ex) {
            promise.fail(ex);
        }
    }

    /**
     * Create the modules for dependency injection.
     *
     * @param vertx The Vertx instance
     * @return Array of modules to use for dependency injection
     */
    private Module[] createModules(Vertx vertx) {
        Module overrideModule = createOverrideModule(vertx);
        if (overrideModule != null) {
            return new Module[] { Modules.override(new QubershipModule(vertx)).with(overrideModule) };
        }
        return new Module[] { new QubershipModule(vertx) };
    }

    /**
     * Create the module for dependency injection.
     * Override this method to add custom module for your application.
     *
     * @param vertx The Vertx instance
     * @return Module to use for dependency injection
     */
    protected Module createOverrideModule(Vertx vertx) {
        return null;
    }

    /**
     * Called when the flow has finished execution.
     *
     * @param main the main verticle
     * @param flowResult the result of the flow execution
     */
    protected void flowFinished(FlowMainVerticle main, AsyncResult<?> flowResult) {
        main.undeploy();    // Fire-and-forget
        if (flowResult.succeeded()) {
            getLogger().info("Flow succeeded. Undeploying and terminating.");
        } else {
            getLogger().error("Flow failed. Undeploying and terminating.", flowResult.cause());
        }
    }


    //------------------------------------------------------
    // Common command-line parameters

    /**
     * Sets a property with the given parameters.
     *
     * @param params array of property parameters in format "name=value"
     */
    @Option(longName = "set", argName = "set", required = false)
    @Description("Universal setter: name=value (multiple)")
    public void setProperty(String[] params) {
        for (String param: params) {
            String[] pair = param.split("=", 2);
            properties.put(pair[0], pair.length > 1 ? pair[1] : "");
        }
    }

    /**
     * Sets the configuration path.
     *
     * @param configPath the path to the configuration folder
     */
    @Option(longName = "configPath", argName = "configPath", shortName = "c", required = false)
    @Description("Path to folder containing the configuration files")
    public void setConfigPath(String configPath) {
        properties.put(CONFIG_PATH_POINTER, configPath);
    }

    /**
     * Sets the profile to be used.
     *
     * @param profile the profile name or file
     */
    @Option(longName = "profile", argName = "profile", shortName = "p", required = false)
    @Description("Custom profile to be used. By default uses properties format, in case of json, use file name with " +
            "extension (Examples: \"custom\", \"custom.properties\", \"custom_example.json\")")
    public void setProfile(String profile) {
        properties.put(PROFILE_POINTER, profile);
    }

}
