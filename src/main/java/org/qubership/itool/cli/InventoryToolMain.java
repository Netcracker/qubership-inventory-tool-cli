/*
 * Copyright 2024-2025 NetCracker Technology Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.qubership.itool.cli;

import org.qubership.itool.cli.spi.CommandProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.util.ServiceLoader;
import java.util.concurrent.Callable;
import io.vertx.core.Vertx;

/**
 * Main entry point for the inventory tool CLI application using Picocli.
 *
 * This class provides the main CLI interface with all available commands. Commands are registered
 * as subcommands and can be discovered dynamically.
 */
@Command(name = "inventory-tool", description = "Inventory tool CLI application",
        mixinStandardHelpOptions = true, versionProvider = InventoryToolMain.VersionProvider.class)
public class InventoryToolMain implements Callable<Integer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(InventoryToolMain.class);

    private CommandLine commandLine;

    private static Vertx sharedVertx;

    public static class VersionProvider implements CommandLine.IVersionProvider {
        @Override
        public String[] getVersion() throws Exception {
            try (var input = InventoryToolMain.class.getClassLoader()
                    .getResourceAsStream("inventory.tool.build.properties")) {
                if (input == null) {
                    throw new IllegalStateException(
                            "Required resource 'inventory.tool.build.properties' not found in classpath.");
                }

                var props = new java.util.Properties();
                props.load(input);
                String version = props.getProperty("inventory-tool.version");
                if (version == null || version.trim().isEmpty()) {
                    throw new IllegalStateException(
                            "Version property 'inventory-tool.version' not found or empty in "
                                    + "inventory.tool.build.properties.");
                }

                return new String[] {version};
            } catch (Exception e) {
                if (e instanceof IllegalStateException) {
                    throw e; // Re-throw our specific exceptions
                }
                throw new IllegalStateException(
                        "Failed to read version from inventory.tool.build.properties: "
                                + e.getMessage(),
                        e);
            }
        }
    }

    public static void main(String[] args) {
        LOGGER.info("=== Inventory Tool CLI ===");

        // Initialize shared Vertx instance
        sharedVertx = Vertx.vertx();

        try {
            // Context will be initialized in execution strategy with actual command configuration
            InventoryToolMain app = new InventoryToolMain();
            CommandLine commandLine = new CommandLine(app);

            // Load and register all extension commands
            loadExtensionCommands(commandLine);

            // Store the CommandLine instance for use in the call method
            app.commandLine = commandLine;

            // Set custom execution strategy to load configuration before command execution
            commandLine.setExecutionStrategy(new ConfigLoadingExecutionStrategy(sharedVertx));

            int exitCode = commandLine.execute(args);
            cleanupSharedResources();
            System.exit(exitCode);
        } catch (Exception e) {
            LOGGER.error("Application failed", e);
            cleanupSharedResources();
            System.exit(1);
        }
    }



    // Cleanup shared resources
    private static void cleanupSharedResources() {
        if (sharedVertx != null) {
            sharedVertx.close();
        }
        ApplicationContextHolder.clear();
    }


    @Override
    public Integer call() throws Exception {
        // This is called when no subcommand is specified
        // Use Picocli's built-in help system to show available commands
        if (commandLine != null) {
            commandLine.usage(System.out);
        } else {
            // Fallback if CommandLine is not available
            CommandLine.usage(this, System.out);
        }
        return 0;
    }

    /**
     * Loads and registers all extension commands using ServiceLoader.
     *
     * @param commandLine the CommandLine instance to add subcommands to
     */
    private static void loadExtensionCommands(CommandLine commandLine) {
        LOGGER.debug("Loading commands via SPI...");

        try {
            ServiceLoader<CommandProvider> commandProviders =
                    ServiceLoader.load(CommandProvider.class);

            int commandCount = 0;
            for (CommandProvider provider : commandProviders) {
                try {
                    String commandName = provider.getCommandName();
                    Object command = provider.createCommand();

                    LOGGER.debug("Found command: {}", commandName);

                    // Add the command to Picocli CommandLine
                    CommandLine subCommandLine = new CommandLine(command);

                    // Configure subcommand with same settings as main command
                    subCommandLine.setOptionsCaseInsensitive(true);
                    subCommandLine.setAbbreviatedOptionsAllowed(true);
                    subCommandLine.setSubcommandsCaseInsensitive(true);
                    subCommandLine.setAbbreviatedSubcommandsAllowed(true);

                    commandLine.addSubcommand(commandName, subCommandLine);

                    commandCount++;
                } catch (Exception e) {
                    LOGGER.warn("Failed to load command from provider: {}",
                            provider.getClass().getName(), e);
                }
            }

            if (commandCount > 0) {
                LOGGER.info("Loaded {} command(s) from SPI providers", commandCount);
            } else {
                LOGGER.debug("No SPI command providers found");
            }

        } catch (Exception e) {
            LOGGER.warn("Failed to discover commands", e);
        }
    }

}
