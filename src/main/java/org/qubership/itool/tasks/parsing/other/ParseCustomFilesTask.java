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

package org.qubership.itool.tasks.parsing.other;

import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.qubership.itool.tasks.parsing.AbstractParseFileTask;
import org.qubership.itool.tasks.parsing.configuration.ParseComponentConfFilesVerticle;
import org.qubership.itool.utils.FSUtils;
import org.qubership.itool.utils.GitUtils;
import org.qubership.itool.utils.YamlParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.qubership.itool.modules.report.GraphReport.EXCEPTION;

/**
 * Store some config files into the graph as "file" and "directory" elements.
 *
 */
public class ParseCustomFilesTask extends ParseComponentConfFilesVerticle {
    protected Logger LOGGER = LoggerFactory.getLogger(ParseCustomFilesTask.class);

    protected String[] getFilePatterns() {
        return new String[]{
            "LICENSE",
            "README"
        };
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }
}
