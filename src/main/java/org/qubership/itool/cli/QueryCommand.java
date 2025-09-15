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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import static org.qubership.itool.utils.ConfigProperties.*;

/**
 * Execute Gremlin query against the graph data.
 */
@Command(
    name = "query",
    description = "Execute Gremlin query",
    mixinStandardHelpOptions = true
)
public class QueryCommand extends AbstractCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(QueryCommand.class);

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

    public QueryCommand() {
        super();
        properties.put(OFFLINE_MODE_PROPERTY, "true");
    }

    @Option(names = {"-f", "--file"}, description = "JSON file that CLI should load instead of Graph dump")
    public void setFile(String file) {
        properties.put(QUERY_FILE_POINTER, file);
    }

    @Option(names = {"-s", "--step"}, description = "Execution step for query (default is 'result' step)")
    public void setStep(String step) {
        properties.put(QUERY_STEP_POINTER, step);
    }

    @Option(names = {"-l", "--login"}, description = "Login to access services requiring authentication")
    public void setLogin(String login) {
        properties.put(LOGIN_PROPERTY, login);
    }

    @Option(names = {"--passwordSource"}, description = "Password source, default: \"file:password.txt\"")
    public void setPasswordSource(String passwordSource) {
        properties.put(PASSWORD_SOURCE_PROPERTY, passwordSource);
    }

    @Option(names = {"--progressPath"}, description = "Path to progress folder (default is 'progress')")
    public void setProgressPath(String progressPath) {
        this.properties.put(QUERY_PROGRESS_PATH_POINTER, progressPath);
    }

    @Override
    public Integer call() throws Exception {
        LOGGER.info("Query command execution");

        // Execute the flow using the existing AbstractCommand infrastructure
        runFlow(new QueryVerticle(), null);

        return 0;
    }
}
