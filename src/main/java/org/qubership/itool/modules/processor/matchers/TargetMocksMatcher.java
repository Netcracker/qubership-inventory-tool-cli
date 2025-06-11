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

import java.util.*;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.qubership.itool.modules.graph.Graph;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;

import static org.qubership.itool.modules.graph.Graph.*;
import static org.qubership.itool.modules.gremlin2.P.*;

/**
 * <p>This class pre-compiles matchers for already existing vertices
 * marked with keys <code>isMock: true</code> and <code>mockedFor</code>.
 *
 * <p>The matchers then try to match every new vertex against those existing vertices.
 *
 * <p>XXX This matcher never excludes the found vertex from further matching.
 * If several non-mock vertices from source graph match the same mock vertex in target graph,
 * merging may fail, but such case is not checked here.
 */
public class TargetMocksMatcher extends CompoundVertexMatcher {

    private static final Logger LOG = LoggerFactory.getLogger(TargetMocksMatcher.class);

    public TargetMocksMatcher(Graph targetGraph) {
        super(getMatchersForTargetGraph(targetGraph));
    }

    public static List<VertexMatcher> getMatchersForTargetGraph(Graph targetGraph) {
        List<JsonObject> mocksInTarget = targetGraph.traversal().V()
                .has(F_MOCK_FLAG, eq(true))
                .hasKey(F_MOCKED_FOR)
                .toList();

        if (LOG.isDebugEnabled()) {
            LOG.debug("Mocked vertices found: {}",
                mocksInTarget.stream().map(v -> v.getString(F_ID)).collect(Collectors.joining(", ", "[", "]")));
        }

        return mocksInTarget.stream()
            .map(mock -> createCorrelatorByExample(targetGraph, mock))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    static VertexMatcher createCorrelatorByExample(Graph targetGraph, JsonObject mock) {
        List<JsonPointer> mockPtrs = getMockedForSet(targetGraph, mock).stream()
            .map(JsonPointer::from)
            .collect(Collectors.toList());
        if (mockPtrs.isEmpty()) {
            return null;
        }

        // New vertex must match all the attributes of the example
        return (srcGraph, newVertex, tgtGraph) -> {
            for (JsonPointer ptr: mockPtrs) {
                Object realVertexValue = ptr.queryJson(newVertex);
                Object mockedVertexValue = ptr.queryJson(mock);
                if (realVertexValue instanceof JsonArray) {
                    if (!((JsonArray)realVertexValue).contains(mockedVertexValue)) {
                        return null;
                    }
                } else if (! Objects.equals(mockedVertexValue, realVertexValue)) {
                    return null;
                }
            }

            LOG.debug("Match found! keysToMatch={}, oldVertex={}, newVertex={}", mockPtrs,
                    mock.getString(F_ID), newVertex.getString(F_ID));
            return mock;
        };
    }

    @SuppressWarnings("unchecked")
    static Set<String> getMockedForSet(Graph origin, JsonObject mock) {
        Object mockedFor = mock.getValue(F_MOCKED_FOR);
        if (mockedFor instanceof JsonArray) {
            return new HashSet<>( ((JsonArray)mockedFor).getList() );
        } else if (mockedFor instanceof String) {
            return Collections.singleton((String)mockedFor);
        } else {
//            throw new InvalidGraphException(origin,
//                    "Vertex " + mock.getString(F_ID) + " contains improper value for " + F_MOCKED_FOR);
            // TODO: Figure out how do we get mock domains and their purpose. They are getting caught with this error
            LOG.error("Vertex " + mock.getString(F_ID) + " contains improper value for " + F_MOCKED_FOR);
            return Collections.emptySet();
        }
    }

}
