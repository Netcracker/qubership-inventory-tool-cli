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

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.qubership.itool.modules.graph.Graph;

import io.vertx.core.json.JsonObject;

import static org.qubership.itool.modules.graph.Graph.*;

public class FileMatcher implements VertexMatcher {

    final Map<String, JsonObject> fileLinkToVertex;

    public FileMatcher(Graph targetGraph) {
        fileLinkToVertex = targetGraph.traversal().V().hasType("file", "directory").toList()
            .stream()
            .filter(f -> f.getString("fileLink") != null)
            .collect(Collectors.toMap(f -> f.getString("fileLink"), Function.identity(), (f1, f2) -> f1));
    }

    @Override
    public JsonObject findExistingVertex(Graph sourceGraph, JsonObject newVertex, Graph targetGraph) {
        String type = newVertex.getString(F_TYPE);
        if (! "file".equals(type) && ! "directory".equals(type)) {
            return null;
        }
        String fileLink = newVertex.getString("fileLink");
        return fileLinkToVertex.get(fileLink);
    }

}
