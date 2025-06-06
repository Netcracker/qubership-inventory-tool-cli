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

package org.qubership.itool.modules.gremlin2.graph;

import org.qubership.itool.modules.graph.BasicGraph;
import org.qubership.itool.modules.gremlin2.GremlinException;
import org.qubership.itool.modules.gremlin2.Traversal;
import org.qubership.itool.modules.gremlin2.step.GraphStep;
import org.qubership.itool.modules.gremlin2.util.ElementType;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class GraphTraversalSource {

    private BasicGraph graph;

    public GraphTraversalSource(BasicGraph graph) {
        this.graph = graph;
    }

    public GraphTraversal<JsonObject, JsonObject> V(Traversal<?, ?> traversal) {
        List vertexIds = new ArrayList();
        List<?> objects = traversal.toList();
        for (Object vertex : objects) {
            if (vertex instanceof String) {
                vertexIds.add(vertex);
            } else if (vertex instanceof JsonObject) {
                JsonObject vrtx = (JsonObject) vertex;
                Object id = vrtx.getValue("id");
                if (id == null) {
                    throw new GremlinException("Inner Traversal result JsonObject must contain \"id\" key");
                }
                vertexIds.add(id);
            } else {
                throw new GremlinException("Inner Traversal result must be String (vertexId) or JsonObject (vertex)");
            }
        }
        return V(vertexIds);
    }

    public GraphTraversal<JsonObject, JsonObject> V(List<String> vertexIds) {
        return V(vertexIds.toArray(new String[vertexIds.size()]));
    }

    public GraphTraversal<JsonObject, JsonObject> V(final String... vertexIds) {
        GraphTraversalSource clone = this.clone();
        GraphTraversal.Admin<JsonObject, JsonObject> traversal = new DefaultGraphTraversal<>(clone, true);
        return traversal.addStep(new GraphStep<>(traversal, JsonObject.class, true, ElementType.vertex, vertexIds));
    }

    public GraphTraversal<JsonObject, JsonObject> E(final String... edgeIds) {
        GraphTraversalSource clone = this.clone();
        GraphTraversal.Admin<JsonObject, JsonObject> traversal = new DefaultGraphTraversal<>(clone, true);
        return traversal.addStep(new GraphStep<>(traversal, JsonObject.class, true, ElementType.edge, edgeIds));
    }

    @SuppressWarnings("CloneDoesntDeclareCloneNotSupportedException")
    public GraphTraversalSource clone() {
        GraphTraversalSource clone = new GraphTraversalSource(this.graph);
        return clone;
    }

    public BasicGraph getGraph() {
        return this.graph;
    }

}
