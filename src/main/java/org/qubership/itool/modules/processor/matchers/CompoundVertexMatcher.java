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

import java.util.List;

import org.qubership.itool.modules.graph.Graph;
import io.vertx.core.json.JsonObject;

public class CompoundVertexMatcher implements VertexMatcher {

    private final VertexMatcher delegates[];

    public CompoundVertexMatcher(VertexMatcher... delegates) {
        this.delegates = delegates;
    }

    public CompoundVertexMatcher(List<VertexMatcher> delegates) {
        this(delegates.toArray( new VertexMatcher[delegates.size()]) );
    }

    @Override
    public JsonObject findExistingVertex(Graph sourceGraph, JsonObject newVertex, Graph targetGraph) {
        for (VertexMatcher delegate: delegates) {
            JsonObject existing = delegate.findExistingVertex(sourceGraph, newVertex, targetGraph);
            if (existing != null) {
                return existing;
            }
        }
        return null;
    }

}
