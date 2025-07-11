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

package org.qubership.itool.tasks.parsing.configuration;

import org.qubership.itool.modules.parsing.InventoryJsonParser;
import org.qubership.itool.tasks.parsing.AbstractParseFileTask;

import io.vertx.core.json.JsonObject;

import org.qubership.itool.utils.FSUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import javax.annotation.Resource;

import jakarta.inject.Provider;

public class ParseInventoryFileVerticle extends AbstractParseFileTask {
    protected Logger LOGGER = LoggerFactory.getLogger(ParseInventoryFileVerticle.class);

    @Resource
    protected Provider<InventoryJsonParser> inventoryJsonParser;

    @Override
    protected String[] getFilePatterns() {
        return new String[] { "inventory.md", "inventory.json" };
    }

    @Override
    protected void parseSingleFile(JsonObject domain, JsonObject component, String fileName) throws IOException {
        getLogger().info("Parsing {} from {}", fileName, component.getString("id"));

        String inventorySource;
        try {
            inventorySource = FSUtils.readFileSafe(fileName);
        } catch (IOException e) {
            System.out.println("Failed to read " + fileName);
            return;
        }

        if (fileName.endsWith("inventory.json")) {
            inventoryJsonParser.get().parse(domain, component, inventorySource);
        }
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

}
