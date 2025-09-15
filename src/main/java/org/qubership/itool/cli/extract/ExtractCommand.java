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

package org.qubership.itool.cli.extract;

import org.qubership.itool.cli.AbstractCommand;
import org.qubership.itool.utils.ConfigUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.Properties;

import static org.qubership.itool.cli.ci.CiConstants.*;
import static org.qubership.itool.utils.ConfigProperties.*;

/**
 * Command for extraction of specific data from graph.
 */
@Command(
    name = "extract",
    description = "Extract Language and Framework data from Graph",
    mixinStandardHelpOptions = true
)
public class ExtractCommand extends AbstractCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExtractCommand.class);

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

    public ExtractCommand() {
        super();
        properties.put(PROFILE_POINTER, "ci");
        properties.put(OFFLINE_MODE_PROPERTY, "true");
        properties.put(SAVE_PROGRESS_PROPERTY, "false");
    }

    @Option(names = {"--inputFile"}, description = "Input file name", required = true)
    public void setInputFile(String inputFile) {
        this.properties.put(P_INPUT_FILE, inputFile);
    }

    @Option(names = {"--outputFile"}, description = "Output file name", required = true)
    public void setOutputFile(String outputFile) {
        this.properties.put(P_OUTPUT_FILE, outputFile);
    }

    @Override
    public Integer call() throws Exception {
        getLogger().info("Graph data extraction flow execution");
        getLogger().info("----- Configuration -----");
        Properties buildProperties = ConfigUtils.getInventoryToolBuildProperties();
        getLogger().info("cli version: {}", buildProperties.get("inventory-tool-cli.version"));
        getLogger().info("inputFile: {}", properties.get(P_INPUT_FILE));
        getLogger().info("outputFile: {}", properties.get(P_OUTPUT_FILE));

        // Execute the flow using the existing AbstractCommand infrastructure
        runFlow(new ExtractMainVerticle(), null);

        return 0;
    }
}
