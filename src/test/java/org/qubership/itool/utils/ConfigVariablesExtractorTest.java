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

package org.qubership.itool.utils;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.IOException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConfigVariablesExtractorTest {

    YamlParser parser;

    @BeforeAll
    public void init() {
        parser = new YamlParser();
    }

    @Test
    void extractVariableNames() {
        Object yamlFileContents = null;
        try {
            yamlFileContents = FSUtils.getYamlFileContents(getClass(), "classpath:/utils/variablesExtractor.yaml", parser);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Set<String> variableNames = ConfigVariablesExtractor.extractVariableNames(yamlFileContents);

        assertEquals(Set.of(
                        "DEPLOYMENT_RESOURCE_NAME",
                        "SERVICE_NAME",
                        "HPA_AVG_CPU_UTILIZATION",
                        "HPA_SCALING_DOWN_INTERVAL",
                        "HPA_MIN_REPLICAS",
                        "HPA_MAX_REPLICAS",
                        "HPA_SCALING_UP_INTERVAL",
                        "TEST_VARIABLE1",
                        "TEST_VARIABLE2")
                , variableNames
        );
    }
}