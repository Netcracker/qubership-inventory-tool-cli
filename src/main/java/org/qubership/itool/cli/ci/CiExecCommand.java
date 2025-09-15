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

import static org.qubership.itool.cli.ci.CiConstants.P_COMP_NAME;
import static org.qubership.itool.cli.ci.CiConstants.P_COMP_VERSION;
import static org.qubership.itool.cli.ci.CiConstants.P_DUMP_BY;
import static org.qubership.itool.cli.ci.CiConstants.P_INPUT_DIRECTORY;
import static org.qubership.itool.cli.ci.CiConstants.P_MOCK_DOMAIN;
import static org.qubership.itool.cli.ci.CiConstants.P_OUTPUT_FILE;
import static org.qubership.itool.cli.ci.CiConstants.P_REPOSITORY;
import static org.qubership.itool.cli.ci.CiConstants.P_RUN_NAME;
import static org.qubership.itool.utils.ConfigProperties.PROFILE_POINTER;
import static org.qubership.itool.utils.ConfigProperties.RELEASE_POINTER;
import java.io.File;
import java.util.Properties;
import org.qubership.itool.cli.FlowMainVerticle;
import org.qubership.itool.modules.graph.GraphDataConstants;
import org.qubership.itool.utils.ConfigUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * A command for single-component run on CI.
 * Works in "ci" profile.
 */
@Command(
    name = "ci-exec",
    description = "CI flow: parse a single component",
    mixinStandardHelpOptions = true
)
public class CiExecCommand extends AbstractCiCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(CiExecCommand.class);

    public static final String DEFAULT_OUTPUT_DIRECTORY_DESKTOP = "progress-ci";
    public static final String DEFAULT_INPUT_DIRECTORY_DOCKER = "/var/input";
    public static final String DEFAULT_OUTPUT_DIRECTORY_DOCKER = "/var/output";

    public CiExecCommand() {
        super();
        // Set CI-exec-specific defaults
        properties.put(P_MOCK_DOMAIN, GraphDataConstants.UNKNOWN_DOMAIN_NAME);
    }

    // CI Exec specific options
    @Option(names = {"--componentName"}, description = "Component name in builder", required = true)
    public void setComponentName(String componentName) {
        properties.put(P_COMP_NAME, componentName);
    }

    @Option(names = {"--componentVersion"}, description = "Component version in builder")
    public void setComponentVersion(String componentVersion) {
        properties.put(P_COMP_VERSION, componentVersion);
    }

    @Option(names = {"--repository"}, description = "Repository of the target component",
            required = true)
    public void setRepository(String repository) {
        if ("null".equals(repository)) {
            getLogger().error("'null' repository name passed");
            System.exit(1);
        }
        properties.put(P_REPOSITORY, repository);
        String runName = repository.replaceFirst("^.*/(.*?)(\\.git)?$", "$1");
        properties.put(P_RUN_NAME, runName);
    }

    @Option(names = {"--outputFile"}, description = "Output file name")
    public void setOutputFile(String outputFile) {
        properties.put(P_OUTPUT_FILE, outputFile);
    }

    @Option(names = {"--dumpResultsBy"},
            description = "Strategy for automatic generation of output file name when it is not provided. " +
                    "Choices: hash, id, repo")
    public void setDumpResultsBy(String dumpResults) {
        properties.put(P_DUMP_BY, dumpResults);
    }

    @Override
    public Integer call() throws Exception {
        getLogger().info("Inventory tool main flow execution for CI");

        logAndFillDirs();

        // Log configuration
        logConfiguration();

        // Execute the flow using the existing AbstractCommand infrastructure
        runFlow(createMainVerticle(), null);

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
        getLogger().info("repository: {}", properties.get(P_REPOSITORY));
        getLogger().info("releaseBranch: {}", properties.get(RELEASE_POINTER));
        getLogger().info("componentName: {}", properties.get(P_COMP_NAME));
        getLogger().info("componentVersion: {}", properties.get(P_COMP_VERSION));
    }

    protected FlowMainVerticle createMainVerticle() {
        String inputDir = properties.get(P_INPUT_DIRECTORY);

        File appInvJson = new File(inputDir, "application_inventory.json");
        boolean appExec = false;
        if (appInvJson.isFile()) {
            getLogger().info("File {} exists!", appInvJson);
            appExec = true;
        } else {
            File appInvJson2 = new File(inputDir, "application-inventory.json");
            if (appInvJson2.isFile()) { // Fallback
                getLogger().warn("File {} exists! Considering it as {}", appInvJson2, "application_inventory.json");
                appExec = true;
            }
        }

        if (appExec) {
            getLogger().info("==> switching to Application flow");
            return new CiExecApplicationVerticle();
        } else {
            getLogger().info("File {} does NOT exist", appInvJson);
            getLogger().info("==> Normal flow");
            return new CiExecVerticle();
        }
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }
}
