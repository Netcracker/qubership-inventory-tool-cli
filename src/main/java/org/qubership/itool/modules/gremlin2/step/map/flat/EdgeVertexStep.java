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

package org.qubership.itool.modules.gremlin2.step.map.flat;

import org.qubership.itool.modules.graph.BasicGraph;
import org.qubership.itool.modules.gremlin2.GremlinException;
import org.qubership.itool.modules.gremlin2.Traversal;
import org.qubership.itool.modules.gremlin2.Traverser;
import org.qubership.itool.modules.gremlin2.step.AbstractStep;
import org.qubership.itool.modules.gremlin2.structure.Direction;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

import static org.qubership.itool.modules.graph.Graph.F_ID;

public class EdgeVertexStep<E extends JsonObject> extends FlatMapStep<JsonObject, JsonObject> { // Edge, Vertex

    protected Direction direction;

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + direction + ")";
    }

    public EdgeVertexStep(Traversal.Admin traversal, Direction direction) {
        super(traversal);
        this.direction = direction;
    }

    @Override
    protected Traverser<JsonObject> generateTraverser(Traverser.Admin<JsonObject> previousTraverser, JsonObject value) {
        Traverser<JsonObject> traverser = previousTraverser.split(value, value, this);
        return traverser;
    }

    @Override
    protected List<JsonObject> flatMap(Traverser.Admin<JsonObject> traverser) {
        List<JsonObject> result = new ArrayList<>();
        BasicGraph graph = traversal.getGraph();
        JsonObject edge = null;
        if (graph != null) {
            edge = traverser.getSource();
            if (edge == null) {
                edge = traverser.get();
            }
        }
        if (edge == null || graph.getEdge(edge.getString(F_ID)) != edge) {
            throw new GremlinException(this.getClass().getSimpleName() + " applicable only to Edge");
        }

        switch (this.direction) {
            case OUT:
                result.add(graph.getEdgeSource(edge.getString(F_ID)));
                break;
            case IN:
                result.add(graph.getEdgeTarget(edge.getString(F_ID)));
                break;
            case BOTH:
                result.add(graph.getEdgeSource(edge.getString(F_ID)));
                result.add(graph.getEdgeTarget(edge.getString(F_ID)));
        }
        return result;
    }

    @Override
    public AbstractStep<JsonObject, JsonObject> clone() {
        EdgeVertexStep clone = (EdgeVertexStep) super.clone();
        clone.direction = this.direction;
        return clone;
    }

}
