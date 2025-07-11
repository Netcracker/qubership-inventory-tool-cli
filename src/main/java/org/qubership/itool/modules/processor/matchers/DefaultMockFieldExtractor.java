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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.qubership.itool.modules.graph.Graph;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import static org.qubership.itool.modules.graph.Graph.F_MOCKED_FOR;
import static org.qubership.itool.modules.graph.Graph.F_ID;

/**
 * Default implementation of MockFieldExtractor.
 * Provides the standard logic for extracting mocked field names from mock vertices.
 */
public class DefaultMockFieldExtractor implements MockFieldExtractor {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultMockFieldExtractor.class);

    @Override
    @SuppressWarnings("unchecked")
    public Set<String> getMockedForSet(Graph origin, JsonObject mock) {
        Object mockedFor = mock.getValue(F_MOCKED_FOR);
        if (mockedFor instanceof JsonArray) {
            return new HashSet<>(((JsonArray) mockedFor).getList());
        } else if (mockedFor instanceof String) {
            return Collections.singleton((String) mockedFor);
        } else {
            // TODO: Figure out how do we get mock domains and their purpose. They are getting caught with this error
            LOG.error("Vertex " + mock.getString(F_ID) + " contains improper value for " + F_MOCKED_FOR);
            return Collections.emptySet();
        }
    }
}
