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

import io.vertx.core.cli.CLIException;
import io.vertx.core.cli.annotations.*;

import java.util.Properties;

import org.qubership.itool.utils.ConfigUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.qubership.itool.cli.ExecCommand;

import static org.qubership.itool.cli.ci.CiConstants.*;
import static org.qubership.itool.utils.ConfigProperties.*;

/**
 * A command for CI assembly.
 * Works in "default" profile, loads domains from it before merging graphs.
 *
 * <p>Run example:
 *<pre>
 * java -jar &lt;JAR&gt; ci-assembly \
 *  -appName=ApplicationName \
 *  -inputDirectory=/path/to/local/results/ \
 *  -outputFile=/path/to/assembly.result.json
 *</pre>
 */
@Name("ci-assembly")
@Summary("CI flow: assembly")
public class CiAssemblyCommand extends ExecCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(CiAssemblyCommand.class);

    public static final String DEFAULT_OUTPUT_DIRECTORY_DESKTOP = "progress";


    protected Logger getLogger() {
        return LOGGER;
    }

    public CiAssemblyCommand() {
        super();
        // "default" to include all domains, "ci" to include internal domains only
        properties.put(PROFILE_POINTER, "ci");
        properties.put(OFFLINE_MODE, "true");
        properties.put(SAVE_PROGRESS, "false");
        properties.put(P_DEFAULT_OUTPUT_DIRECTORY, DEFAULT_OUTPUT_DIRECTORY_DESKTOP);
        properties.put(P_OUTPUT_FILE, "assembly.result.json");
    }

    @Override
    public void run() throws CLIException {
        getLogger().info("Inventory tool assembly flow execution for CI");
        getLogger().info("----- Configuration -----");
        Properties buildProperties = ConfigUtils.getInventoryToolBuildProperties();
        getLogger().info("cli version: {}", buildProperties.get("inventory-tool-cli.version"));
        getLogger().info("profile: {}", properties.get(PROFILE_POINTER));
        logAndFillDirs();
        getLogger().info("outputFile: {}", properties.get(P_OUTPUT_FILE));
        getLogger().info("appName: {}", properties.get(P_APP_NAME));
        getLogger().info("appVersion: {}", properties.get(P_APP_VERSION));

        runFlow(new CiAssemblyVerticle(), null);
    }

    @Option(longName = "inputDirectory", argName = "inputDirectory", required = false)
    @Description("Input directory. All files within it will be loaded and merged.")
    public void setInputDirectory(String inputDirectory) {
        this.properties.put(P_INPUT_DIRECTORY, inputDirectory);
    }

    @Option(longName = "outputDirectory", argName = "outputDirectory", required = false)
    @Description("Output directory for resulting graph")
    public void setOutputDirectory(String outputDirectory) {
        this.properties.put(P_OUTPUT_DIRECTORY, outputDirectory);
    }

    @Option(longName = "outputFile", argName = "outputFile", required = false)
    @Description("Output file name for resulting graph")
    public void setOutputFile(String outputFile) {
        this.properties.put(P_OUTPUT_FILE, outputFile);
    }

    @Option(longName = "appName", argName = "appName", shortName = "appName", required = true)
    @Description("Application name, e.g.: \"Inventory-Tool\"")
    public void setAppName(String appName) {
        properties.put(P_APP_NAME, appName);
    }

    @Option(longName = "appVersion", argName = "appVersion", shortName = "appVersion", required = false)
    @Description("Application version from builder, e.g.: \"main-SNAPSHOT\"")
    public void setAppVersion(String appVersion) {
        properties.put(P_APP_VERSION, appVersion);
    }

    @Option(longName = "dockerMode", shortName = "docker", argName = "dockerMode", required = false)
    @Description("Docker mode: true/false")
    public void setDockerMode(boolean dockerMode) {
        properties.put(P_DEFAULT_OUTPUT_DIRECTORY, dockerMode ? DEFAULT_OUTPUT_DIRECTORY_DOCKER : DEFAULT_OUTPUT_DIRECTORY_DESKTOP);
        super.setDockerMode(dockerMode);
    }

}
