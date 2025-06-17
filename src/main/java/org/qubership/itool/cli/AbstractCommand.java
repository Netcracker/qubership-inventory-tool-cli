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

import io.vertx.core.*;
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
import org.slf4j.LoggerFactory;

import org.qubership.itool.cli.config.ConfigProvider;
import org.qubership.itool.context.FlowContext;
import org.qubership.itool.context.FlowContextFactory;
import org.qubership.itool.factories.JavaAppContextVerticleFactory;

import static org.qubership.itool.utils.ConfigProperties.CONFIG_PATH_POINTER;
import static org.qubership.itool.utils.ConfigProperties.PROFILE_POINTER;


public abstract class AbstractCommand extends ClasspathHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractCommand.class);

    protected Map<String, String> properties = new HashMap<>(Map.of(
            PROFILE_POINTER, "dev",
            CONFIG_PATH_POINTER, "inventory-tool"
    ));

    protected Logger getLogger() {
        return LOGGER;
    }

    /** Create VertX, start a flow using {@link #properties} and config, let the flow complete, terminate JVM
     *
     * @param main Main verticle
     * @param graphService Graph Service to work with. May be omitted (set to null)
     */
    public void runFlow(FlowMainVerticle main, GraphService graphService) {
        Vertx vertx = Vertx.vertx();
        vertx.exceptionHandler(err -> {
            LOGGER.error("Critical error, application is stopping", err);
            System.exit(1);
        });

        runFlow(vertx, main, graphService)
            .onComplete(ar -> System.exit(0));
    }

    protected Future<?> runFlow(Vertx vertx, FlowMainVerticle main, GraphService graphService) {
        ConfigProvider configProvider = new ConfigProvider(properties, vertx);
        return Future.future(promise ->
            configProvider.handleConfig(ar -> configLoaded(vertx, main, graphService, ar.result(), promise)));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected void configLoaded(Vertx vertx, FlowMainVerticle main, GraphService graphService, JsonObject config, Promise promise) {
        try {
            FlowContextFactory factory = FlowContextFactory.create(config);
            FlowContext flowContext = factory.createFlowContext(graphService);

            // XXX Still needs reviewing for real multi-flow design
            Optional<VerticleFactory> verticleFactory = vertx.verticleFactories()
                    .stream()
                    .filter(f -> f instanceof JavaAppContextVerticleFactory)
                    .findAny();
            JavaAppContextVerticleFactory javaTaskFactory;
            if (verticleFactory.isEmpty()) {
                javaTaskFactory = new JavaAppContextVerticleFactory(flowContext, config);
                vertx.registerVerticleFactory(javaTaskFactory);
            } else {
                javaTaskFactory = (JavaAppContextVerticleFactory) verticleFactory.get();
            }

            factory.initializeFlowContext(flowContext, vertx, config);
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

    @Option(longName = "set", argName = "set", required = false)
    @Description("Universal setter: name=value (multiple)")
    public void setProperty(String[] params) {
        for (String param: params) {
            String[] pair = param.split("=", 2);
            properties.put(pair[0], pair.length > 1 ? pair[1] : "");
        }
    }

    @Option(longName = "configPath", argName = "configPath", shortName = "c", required = false)
    @Description("Path to folder containing the configuration files")
    public void setConfigPath(String configPath) {
        properties.put(CONFIG_PATH_POINTER, configPath);
    }

    @Option(longName = "profile", argName = "profile", shortName = "p", required = false)
    @Description("Custom profile to be used. By default uses properties format, in case of json, use file name with " +
            "extension (Examples: \"custom\", \"custom.properties\", \"custom_example.json\")")
    public void setProfile(String profile) {
        properties.put(PROFILE_POINTER, profile);
    }

}
