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

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.qubership.itool.cli.config.ConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.IExecutionStrategy;
import picocli.CommandLine.ParseResult;
import picocli.CommandLine.RunLast;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * Custom execution strategy that loads configuration before executing commands.
 */
public class ConfigLoadingExecutionStrategy implements IExecutionStrategy {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ConfigLoadingExecutionStrategy.class);

    private final Vertx vertx;

    public ConfigLoadingExecutionStrategy(Vertx vertx) {
        this.vertx = vertx;
    }

    /**
     * Load configuration as a Future for async handling.
     *
     * @param properties the properties to use for configuration
     * @return Future containing the loaded configuration
     */
    private Future<JsonObject> loadConfiguration(Map<String, String> properties) {
        if (vertx == null) {
            throw new IllegalStateException("Vertx instance not initialized");
        }

        return Future.future(promise -> {
            ConfigProvider configProvider = new ConfigProvider(properties, vertx);
            configProvider.handleConfig(promise::handle);
        });
    }

    @Override
    public int execute(ParseResult parseResult) {
        // Check if we have a command to execute (not just help or version)
        if (parseResult.hasSubcommand()) {
            ParseResult subcommandParseResult = parseResult.subcommand();
            Object command = subcommandParseResult.commandSpec().userObject();

            // If it's an AbstractCommand, load configuration first
            if (command instanceof AbstractCommand) {
                AbstractCommand abstractCommand = (AbstractCommand) command;

                try {
                    LOGGER.debug("Initializing context with configuration for command: {}",
                            command.getClass().getSimpleName());

                    // Load configuration synchronously
                    Future<JsonObject> configFuture = loadConfiguration(abstractCommand.properties);
                    JsonObject config =
                            configFuture.toCompletionStage().toCompletableFuture().get();

                    ApplicationContextHolder.initializeWithConfig(vertx, config);

                    LOGGER.debug("Context initialized successfully for command: {}",
                            command.getClass().getSimpleName());
                } catch (IllegalStateException e) {
                    LOGGER.error("Vertx not initialized: {}", e.getMessage());
                    return 2; // Configuration error
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof TimeoutException) {
                        LOGGER.error("Configuration loading timed out: {}", e.getMessage());
                        return 3; // Timeout error
                    } else {
                        LOGGER.error("Configuration loading failed: {}", e.getMessage(), e);
                        return 4; // Configuration loading error
                    }
                } catch (InterruptedException e) {
                    LOGGER.error("Configuration loading was interrupted: {}", e.getMessage());
                    Thread.currentThread().interrupt(); // Restore interrupt status
                    return 5; // Interruption error
                } catch (Exception e) {
                    LOGGER.error("Unexpected error during context initialization: {}", e.getMessage(), e);
                    return 6; // Unexpected error
                }
            }
        }

        // Execute the command using the default strategy
        try {
            return new RunLast().execute(parseResult);
        } catch (Exception e) {
            LOGGER.error("Command execution failed", e);
            return 1;
        }
    }
}
