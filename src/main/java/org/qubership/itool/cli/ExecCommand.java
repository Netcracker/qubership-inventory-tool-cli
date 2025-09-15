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

import static org.qubership.itool.utils.ConfigProperties.DISABLED_FEATURES_PROPERTY;
import static org.qubership.itool.utils.ConfigProperties.EXCEL_EXPORT_PROPERTY;
import static org.qubership.itool.utils.ConfigProperties.INCLUDE_DOMAINS_PROPERTY;
import static org.qubership.itool.utils.ConfigProperties.LAST_STEP_PROPERTY;
import static org.qubership.itool.utils.ConfigProperties.LOGIN_PROPERTY;
import static org.qubership.itool.utils.ConfigProperties.OFFLINE_MODE_PROPERTY;
import static org.qubership.itool.utils.ConfigProperties.PASSWORD_SOURCE_PROPERTY;
import static org.qubership.itool.utils.ConfigProperties.PRIOR_RELEASE_POINTER;
import static org.qubership.itool.utils.ConfigProperties.RELEASE_BRANCH_POINTER;
import static org.qubership.itool.utils.ConfigProperties.RELEASE_POINTER;
import static org.qubership.itool.utils.ConfigProperties.SAVE_PROGRESS_PROPERTY;
import static org.qubership.itool.utils.ConfigProperties.START_STEP_PROPERTY;
import static org.qubership.itool.utils.ConfigProperties.UPLOAD_CONFLUENCE_PAGES_POINTER;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Main flow execution command.
 */
@Command(
    name = "exec",
    description = "Execute inventory-tool",
    mixinStandardHelpOptions = true
)
public class ExecCommand extends AbstractCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecCommand.class);

    // ExecCommand specific options
    @Option(names = {"-l", "--login"},
            description = "Login to access services requiring authentication")
    public void setLogin(String login) {
        properties.put(LOGIN_PROPERTY, login);
    }

    @Option(names = {"-e", "--excelExport"}, description = "Path pattern for excel report export")
    public void setExcelExport(String excelExport) {
        properties.put(EXCEL_EXPORT_PROPERTY, excelExport);
    }

    @Option(names = {"-u", "--uploadConfluencePages"},
            description = "List of the page titles to be uploaded to Confluence. " +
                    "Delimiter is ','. Examples: all; none; type:report; \"Tech of DOMAIN1, Cloud Libraries list\"")
    public void setUploadConfluencePages(String uploadConfluencePages) {
        properties.put(UPLOAD_CONFLUENCE_PAGES_POINTER, uploadConfluencePages);
    }

    @Option(names = {"-sp", "--saveProgress"},
            description = "Save execution progress. That allow restart progress from the specified step.")
    public void setSaveProgress(String saveProgress) {
        properties.put(SAVE_PROGRESS_PROPERTY, saveProgress);
    }

    @Option(names = {"-ss", "--startStep"},
            description = "Start execution from the specified step if progress was saved before. " +
                    "See 'saveProgress' property.")
    public void setStartStep(String startStep) {
        properties.put(START_STEP_PROPERTY, startStep);
    }

    @Option(names = {"-ls", "--lastStep"},
            description = "Last execution step (if progress was saved before). See 'saveProgress' property.")
    public void setLastStep(String lastStep) {
        properties.put(LAST_STEP_PROPERTY, lastStep);
    }

    @Option(names = {"-id", "--includeDomains"},
            description = "List of Domains that must be processed. Delimiter is ','")
    public void setIncludeDomains(String includeDomains) {
        properties.put(INCLUDE_DOMAINS_PROPERTY, includeDomains);
    }

    @Option(names = {"-df", "--disabledFeatures"},
            description = "List of the disabled features. Delimiter is ','. " +
                    "Examples: confluenceGenerate,confluenceUpload,excelExport,mavenDependency,repositoryUpdate")
    public void setDisabledFeatures(String disabledFeatures) {
        properties.put(DISABLED_FEATURES_PROPERTY, disabledFeatures);
    }

    @Option(names = {"-r", "--release"},
            description = "Release version to be used as a suffix during the export to Confluence")
    public void setRelease(String release) {
        properties.put(RELEASE_POINTER, release);
    }

    @Option(names = {"-rb", "--releaseBranch"},
            description = "Release version to be used as a suffix during the export to Confluence")
    public void setReleaseBranch(String releaseBranch) {
        properties.put(RELEASE_BRANCH_POINTER, releaseBranch);
    }

    @Option(names = {"-pr", "--priorRelease"},
            description = "Release version prior to selected release to compare to")
    public void setPriorRelease(String priorRelease) {
        properties.put(PRIOR_RELEASE_POINTER, priorRelease);
    }

    @Option(names = {"-pws", "--passwordSource"},
            description = "Password source, e.g.: \"file:password.txt\"")
    public void setPasswordSource(String passwordSource) {
        properties.put(PASSWORD_SOURCE_PROPERTY, passwordSource);
    }

    @Option(names = {"--offline"}, description = "Offline mode: true/false")
    public void setOfflineMode(String offlineMode) {
        properties.put(OFFLINE_MODE_PROPERTY, offlineMode);
    }

    @Override
    public Integer call() throws Exception {
        LOGGER.info("Inventory tool main flow execution");

        // Execute the flow using the existing AbstractCommand infrastructure
        runFlow(new ExecVerticle(), null);

        return 0;
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

}
