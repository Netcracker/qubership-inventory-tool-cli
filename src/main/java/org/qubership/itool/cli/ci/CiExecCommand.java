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

import java.io.File;
import java.util.Properties;

import io.vertx.core.cli.CLIException;
import io.vertx.core.cli.annotations.*;

import org.qubership.itool.modules.graph.GraphDataConstants;
import org.qubership.itool.modules.graph.GraphService;
import org.qubership.itool.utils.ConfigUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.qubership.itool.cli.ExecCommand;
import org.qubership.itool.cli.FlowMainVerticle;

import static org.qubership.itool.cli.ci.CiConstants.*;
import static org.qubership.itool.utils.ConfigProperties.*;

/**
 * A command for single-component run on CI.
 * Works in "ci" profile.
 *
 * <p>Run example:
 *<pre>
 * java -jar &lt;JAR&gt; ci-exec \
 *  --inputDirectory /path/to/local/repositories/ABC/ABCDE \
 *  --repository https://git.your.host/abc/abcde \
 *  --componentName=abcde
 *  --rb feature-branch
 *  --outputFile /path/to/result-abcde.json
 *</pre>
 */
@Name("ci-exec")
@Summary("CI flow: parse a single component")
public class CiExecCommand extends ExecCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(CiExecCommand.class);

    public static final String DEFAULT_OUTPUT_DIRECTORY_DESKTOP = "progress-ci";


    protected Logger getLogger() {
        return LOGGER;
    }

    public CiExecCommand() {
        super();
        properties.put(P_MOCK_DOMAIN, GraphDataConstants.UNKNOWN_DOMAIN_NAME);
        properties.put(PROFILE_POINTER, "ci");
        properties.put(OFFLINE_MODE, "true");
        properties.put(SAVE_PROGRESS, "false");
        properties.put(P_DEFAULT_OUTPUT_DIRECTORY, DEFAULT_OUTPUT_DIRECTORY_DESKTOP);
    }

    @Override
    public void run() throws CLIException {
        getLogger().info("Inventory tool main flow execution for CI");
        getLogger().info("----- Configuration -----");
        Properties buildProperties = ConfigUtils.getInventoryToolBuildProperties();
        getLogger().info("cli version: {}", buildProperties.get("inventory-tool-cli.version"));
        getLogger().info("profile: {}", properties.get(PROFILE_POINTER));
        logAndFillDirs();
        getLogger().info("outputFile: {}", properties.get(P_OUTPUT_FILE));
        getLogger().info("repository: {}", properties.get(P_REPOSITORY));
        getLogger().info("releaseBranch: {}", properties.get(RELEASE_POINTER));
        getLogger().info("componentName: {}", properties.get(P_COMP_NAME));
        getLogger().info("componentVersion: {}", properties.get(P_COMP_VERSION));

        GraphService graphService = null;
        runFlow(createMainVerticle(), graphService);

        /*
        //----- A sample code to run several flows simultaneously
        GraphServiceImpl graphService = new GraphServiceImpl(new FileGraphBuilderImpl());

        Vertx vertx = Vertx.vertx();
        vertx.exceptionHandler(err -> {
            LOGGER.error("Critical error, application is stopping", err);
            System.exit(1);
        });
        getLogger().info("------------------ Starting flow #1 ------------------");
        Future<?> f1 = runFlow(vertx, new CiExecVerticle(), graphService)
                .onComplete(ar -> getLogger().info("------------------ Flow #1 finished ------------------"));
        getLogger().info("------------------ Starting flow #2 ------------------");
        Future<?> f2 = runFlow(vertx, new CiExecVerticle(), graphService)
                .onComplete(ar -> getLogger().info("------------------ Flow #2 finished ------------------"));

        CompositeFuture.join(f1, f2)
            .onComplete(ar -> System.exit(0));
        */
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


    @Option(longName = "componentName", argName = "cn", required = true)
    @Description("Component name in builder")
    public void setComponentName(String componentName) {
        properties.put(P_COMP_NAME, componentName);
    }

    @Option(longName = "componentVersion", argName = "cv", required = false)
    @Description("Component version in builder")
    public void setComponentVersion(String componentVersion) {
        properties.put(P_COMP_VERSION, componentVersion);
    }

    @Option(longName = "repository", argName = "repository", required = true)
    @Description("Repository of the target component")
    public void setRepository(String repository) {
        if ("null".equals(repository)) {
            LOGGER.error("'null' repository name passed");
            System.exit(1);
        }
        properties.put(P_REPOSITORY, repository);
        String runName = repository.replaceFirst("^.*/(.*?)(\\.git)?$", "$1");
        properties.put(P_RUN_NAME, runName);
    }

    @Option(longName = "inputDirectory", argName = "inputDirectory", required = false)
    @Description("Input directory with sources of the target component")
    public void setInputDirectory(String inputDirectory) {
        this.properties.put(P_INPUT_DIRECTORY, inputDirectory);
    }

    @Option(longName = "outputDirectory", argName = "outputDirectory", required = false)
    @Description("Output directory")
    public void setOutputDirectory(String outputDirectory) {
        this.properties.put(P_OUTPUT_DIRECTORY, outputDirectory);
    }

    @Option(longName = "outputFile", argName = "outputFile", required = false)
    @Description("Output file name")
    public void setOutputFile(String outputFile) {
        this.properties.put(P_OUTPUT_FILE, outputFile);
    }

    @Option(longName = "dumpResultsBy", argName = "dumpResultsBy", required = false,
            choices = { DUMP_BY_HASH, DUMP_BY_ID, DUMP_BY_REPO })
    @Description("Strategy for automatic generation of output file name when it is not provided")
    public void setDumpResultsBy(String dumpResults) {
        properties.put(P_DUMP_BY, dumpResults);
    }

    @Option(longName = "dockerMode", shortName = "docker", argName = "dockerMode", required = false)
    @Description("Docker mode: true/false")
    public void setDockerMode(boolean dockerMode) {
        properties.put(P_DEFAULT_OUTPUT_DIRECTORY, dockerMode ? DEFAULT_OUTPUT_DIRECTORY_DOCKER : DEFAULT_OUTPUT_DIRECTORY_DESKTOP);
        super.setDockerMode(dockerMode);
    }

}
