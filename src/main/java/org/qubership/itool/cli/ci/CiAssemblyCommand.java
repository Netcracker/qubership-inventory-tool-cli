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

package org.qubership.itool.cli.ci;

import static org.qubership.itool.cli.ci.CiConstants.P_APP_NAME;
import static org.qubership.itool.cli.ci.CiConstants.P_APP_VERSION;
import static org.qubership.itool.cli.ci.CiConstants.P_OUTPUT_FILE;
import static org.qubership.itool.utils.ConfigProperties.PROFILE_POINTER;
import java.util.Properties;
import org.qubership.itool.utils.ConfigUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * A command for executing CI assembly.
 * Works in "ci" profile, loads domains from it before merging graphs.
 */
@Command(
    name = "ci-assembly",
    description = "CI flow: assembly",
    mixinStandardHelpOptions = true
)
public class CiAssemblyCommand extends AbstractCiCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(CiAssemblyCommand.class);

    public CiAssemblyCommand() {
        super();
        // Set CI assembly-specific defaults
        properties.put(P_OUTPUT_FILE, "assembly.result.json");
    }

    @Option(names = {"--outputFile"}, description = "Output file name for resulting graph")
    public void setOutputFile(String outputFile) {
        properties.put(P_OUTPUT_FILE, outputFile);
    }

    @Option(names = {"--appName"}, description = "Application name, e.g.: \"Inventory-Tool\"",
            required = true)
    public void setAppName(String appName) {
        properties.put(P_APP_NAME, appName);
    }

    @Option(names = {"--appVersion"},
            description = "Application version from builder, e.g.: \"main-SNAPSHOT\"")
    public void setAppVersion(String appVersion) {
        properties.put(P_APP_VERSION, appVersion);
    }

    @Override
    public Integer call() throws Exception {
        getLogger().info("Inventory tool assembly flow execution for CI (Picocli version)");

        logAndFillDirs();

        // Log configuration
        logConfiguration();

        // Execute the flow using the existing AbstractCommand infrastructure
        runFlow(new CiAssemblyVerticle(), null);

        return 0;
    }

    /**
     * Logs the current configuration for debugging purposes.
     */
    private void logConfiguration() {
        getLogger().info("----- Configuration -----");
        Properties buildProperties = ConfigUtils.getInventoryToolBuildProperties();
        getLogger().info("cli version: {}", buildProperties.get("inventory-tool-cli.version"));
        getLogger().info("profile: {}", properties.get(PROFILE_POINTER));
        getLogger().info("outputFile: {}", properties.get(P_OUTPUT_FILE));
        getLogger().info("appName: {}", properties.get(P_APP_NAME));
        getLogger().info("appVersion: {}", properties.get(P_APP_VERSION));
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }
}
