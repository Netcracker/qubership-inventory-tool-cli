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

import org.qubership.itool.cli.AbstractCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Option;
import static org.qubership.itool.cli.ci.CiConstants.P_INPUT_DIRECTORY;
import static org.qubership.itool.cli.ci.CiConstants.P_OUTPUT_DIRECTORY;
import static org.qubership.itool.utils.ConfigProperties.DOCKER_MODE_PROPERTY;
import static org.qubership.itool.utils.ConfigProperties.OFFLINE_MODE_PROPERTY;
import static org.qubership.itool.utils.ConfigProperties.PROFILE_POINTER;
import static org.qubership.itool.utils.ConfigProperties.SAVE_PROGRESS_PROPERTY;
import static org.qubership.itool.cli.ci.CiConstants.P_DEFAULT_OUTPUT_DIRECTORY;

/**
 * Abstract command for CI commands. Extends ExecCommand to inherit the common functionality.
 * Not intended to be used directly, but rather extended by specific CI commands.
 */
public abstract class AbstractCiCommand extends AbstractCommand {

    public static final String DEFAULT_INPUT_DIRECTORY_DOCKER = "/var/input";
    public static final String DEFAULT_OUTPUT_DIRECTORY_DOCKER = "/var/output";
    public static final String DEFAULT_OUTPUT_DIRECTORY_DESKTOP = "progress";

    public AbstractCiCommand() {
        super();
        // Set CI-specific defaults
        properties.put(PROFILE_POINTER, "ci");
        properties.put(OFFLINE_MODE_PROPERTY, "true");
        properties.put(SAVE_PROGRESS_PROPERTY, "false");
        properties.put(P_DEFAULT_OUTPUT_DIRECTORY, DEFAULT_OUTPUT_DIRECTORY_DESKTOP);
    }

    @Option(names = {"--inputDirectory"}, description = "Input directory path")
    public void setInputDirectory(String inputDirectory) {
        properties.put(P_INPUT_DIRECTORY, inputDirectory);
    }

    @Option(names = {"--outputDirectory"}, description = "Output directory path")
    public void setOutputDirectory(String outputDirectory) {
        properties.put(P_OUTPUT_DIRECTORY, outputDirectory);
    }

    @Option(names = {"--defaultOutputDirectory"}, description = "Default output directory path")
    public void setDefaultOutputDirectory(String defaultOutputDirectory) {
        properties.put(P_DEFAULT_OUTPUT_DIRECTORY, defaultOutputDirectory);
    }

    @Option(names = {"--docker", "--dockerMode"}, description = "Docker mode: true/false")
    public void setDockerMode(boolean dockerMode) {
        properties.put(P_DEFAULT_OUTPUT_DIRECTORY,
                dockerMode ? DEFAULT_OUTPUT_DIRECTORY_DOCKER : DEFAULT_OUTPUT_DIRECTORY_DESKTOP);
        properties.put(DOCKER_MODE_PROPERTY, String.valueOf(dockerMode));
    }

    /**
     * Processing for default CI directories.
     * When flow runs, "ciInputDirectory" and "ciOutputDirectory" may be taken from "ci.properties" file.
     * This method helps the command to fall back to default directories before the flow starts.
     */
    protected void logAndFillDirs() {
        boolean isDocker = Boolean.parseBoolean(properties.getOrDefault(DOCKER_MODE_PROPERTY, "false"));
        getLogger().info("dockerMode: {}", isDocker);

        String defInputDirectory = getDefaultInputDir(isDocker);
        String defOutputDirectory = getDefaultOutputDir(isDocker);

        getLogger().info("explicit inputDirectory: {}", properties.get(P_INPUT_DIRECTORY));
        getLogger().info("default inputDirectory: {}", defInputDirectory);
        getLogger().info("explicit outputDirectory: {}", properties.get(P_OUTPUT_DIRECTORY));
        getLogger().info("default outputDirectory: {}", defOutputDirectory);

        if (!properties.containsKey(P_INPUT_DIRECTORY)) {
            if (defInputDirectory != null) {
                properties.put(P_INPUT_DIRECTORY, DEFAULT_INPUT_DIRECTORY_DOCKER);
            } else {
                getLogger().error("Either --docker=true or --inputDirectory must be specified! EXITTING!");
                System.exit(1);
            }
        }
        if (!properties.containsKey(P_OUTPUT_DIRECTORY)) {
            if (defOutputDirectory != null) {
                properties.put(P_OUTPUT_DIRECTORY, defOutputDirectory);
            }
        }
    }

    protected String getDefaultInputDir(boolean isDocker) {
        return isDocker ? DEFAULT_INPUT_DIRECTORY_DOCKER : null;
    }

    protected String getDefaultOutputDir(boolean isDocker) {
        return properties.get(P_DEFAULT_OUTPUT_DIRECTORY);
    }

}
