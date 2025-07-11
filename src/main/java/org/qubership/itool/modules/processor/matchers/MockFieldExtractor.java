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

package org.qubership.itool.modules.processor.matchers;

import java.util.Set;

import org.qubership.itool.modules.graph.Graph;

import io.vertx.core.json.JsonObject;

/**
 * Strategy interface for extracting mocked field names from mock vertices.
 * Allows for extensible and customizable mock field extraction logic.
 */
public interface MockFieldExtractor {

    /**
     * Extracts the set of mocked field names from a mock vertex.
     *
     * @param origin The graph containing the mock vertex (for error context)
     * @param mock The mock vertex containing the mockedFor field
     * @return Set of field names that are being mocked, or empty set if invalid
     */
    Set<String> getMockedForSet(Graph origin, JsonObject mock);
}
