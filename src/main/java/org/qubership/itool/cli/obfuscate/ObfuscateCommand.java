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

package org.qubership.itool.cli.obfuscate;

import java.util.Properties;

import org.qubership.itool.utils.ConfigUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.qubership.itool.cli.ExecCommand;
import org.qubership.itool.cli.ci.CiConstants;

import io.vertx.core.cli.CLIException;
import io.vertx.core.cli.annotations.Description;
import io.vertx.core.cli.annotations.Name;
import io.vertx.core.cli.annotations.Option;
import io.vertx.core.cli.annotations.Summary;

import static org.qubership.itool.cli.ci.CiConstants.*;
import static org.qubership.itool.utils.ConfigProperties.*;


/**
 * A command for obfuscation of CI run or assembly.
 *
 * <p>Run example:
 *<pre>
 * java -jar &lt;JAR&gt; ci-obfuscate \
 *  -inputFile=/path/to/assembly.result.json \
 *  -outputFile=/path/to/obfuscate.result.json
 *</pre>
 */
@Name("ci-obfuscate")
@Summary("Obfuscate Graph")
public class ObfuscateCommand extends ExecCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(ObfuscateCommand.class);

    protected Logger getLogger() {
        return LOGGER;
    }

    public ObfuscateCommand() {
        super();
        properties.put(PROFILE_POINTER, "ci");
        properties.put(OFFLINE_MODE, "true");
        properties.put(SAVE_PROGRESS, "false");
    }

    @Override
    public void run() throws CLIException {
        getLogger().info("Obfuscation main flow execution");
        getLogger().info("----- Configuration -----");
        Properties buildProperties = ConfigUtils.getInventoryToolBuildProperties();
        getLogger().info("cli version: {}", buildProperties.get("inventory-tool-cli.version"));
        getLogger().info("profile: {}", properties.get(PROFILE_POINTER));
        getLogger().info("inputDirectory: {}", properties.get(P_INPUT_DIRECTORY));
        getLogger().info("inputFile: {}", properties.get(P_INPUT_FILE));
        getLogger().info("explicit outputDirectory: {}", properties.get(P_OUTPUT_DIRECTORY));
        getLogger().info("outputFile: {}", properties.get(P_OUTPUT_FILE));

        runFlow(new ObfuscationMainVerticle(), null);
    }

    @Option(longName = "inputDirectory", argName = "inputDirectory", required = false)
    @Description("Input directory")
    public void setInputDirectory(String inputDirectory) {
        this.properties.put(P_INPUT_DIRECTORY, inputDirectory);
    }

    @Option(longName = "inputFile", argName = "inputFile", required = true)
    @Description("Input file name")
    public void setInputFile(String inputFile) {
        this.properties.put(P_INPUT_FILE, inputFile);
    }

    @Option(longName = "outputDirectory", argName = "outputDirectory", required = false)
    @Description("Output directory")
    public void setOutputDirectory(String outputDirectory) {
        this.properties.put(P_OUTPUT_DIRECTORY, outputDirectory);
    }

    @Option(longName = "outputFile", argName = "outputFile", required = true)
    @Description("Output file name")
    public void setOutputFile(String outputFile) {
        this.properties.put(P_OUTPUT_FILE, outputFile);
    }

    @Option(longName = "obfuscationRules", argName = "obfuscationRules", shortName = "rules", required = false)
    @Description("Path to the obfuscated graph dump")
    public void setObfuscationRules(String obfuscationRules) {
        this.properties.put(CiConstants.OBFUSCATION_RULES, obfuscationRules);
    }

}
