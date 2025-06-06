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

import org.qubership.itool.modules.graph.Graph;
import org.qubership.itool.modules.gremlin2.graph.GraphTraversal;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.qubership.itool.modules.graph.Graph.*;
import static org.qubership.itool.modules.gremlin2.P.*;

/**
 * <p>This class tries to match new vertices marked with keys <code>isMock: true</code>
 * and <code>mockedFor</code> with existing vertices from the target graph.
 */
public class SourceMocksMatcher implements VertexMatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(SourceMocksMatcher.class);


    @Override
    public JsonObject findExistingVertex(Graph sourceGraph, JsonObject newVertex, Graph targetGraph) {
        if (! Boolean.TRUE.equals(newVertex.getValue(F_MOCK_FLAG))) {
            return null;
        }

        Set<String> mockedForSet = TargetMocksMatcher.getMockedForSet(sourceGraph, newVertex);
        if (mockedForSet.isEmpty()) {
            return null;
        }

        GraphTraversal<JsonObject, JsonObject> query = targetGraph.traversal().V().has(F_MOCK_FLAG, eq(false));
        for (String mockedFor: mockedForSet) {
            query = query.has(mockedFor, containing(JsonPointer.from(mockedFor).queryJson(newVertex)));
        }
        JsonObject result = query.next();

        if (result != null) {
            LOGGER.debug("Match found! keysToMatch={}, oldVertex={}, newVertex={}", mockedForSet,
                    result.getString(F_ID), newVertex.getString(F_ID));
        }
        return result;
    }

}
