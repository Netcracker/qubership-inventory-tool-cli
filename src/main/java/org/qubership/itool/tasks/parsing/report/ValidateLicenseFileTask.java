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

package org.qubership.itool.tasks.parsing.report;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;
import org.qubership.itool.utils.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.qubership.itool.modules.graph.Graph.F_ID;

public class ValidateLicenseFileTask extends AbstractValidateFileTask {
    protected Logger LOGGER = LoggerFactory.getLogger(AbstractValidateFileTask.class);

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }
    @Override
    protected String getFilePattern() {
        return "LICENSE";
    }

    @Override
    protected void validateFile(JsonObject component, String fileContents) {
        JsonObject validationResult = JsonUtils.getOrCreateJsonObject(component, JsonPointer.from("/validationResults/license"));
        validationResult.put("header", "/License");
        if (fileContents == null) {
            getLogger().info("No license file found for component {}", component.getString(F_ID));
            validationResult
                    .put("result", "Fail")
                    .put("details", "File is missing");
            return;
        }
        validationResult.put("result", "Pass");
    }
}
