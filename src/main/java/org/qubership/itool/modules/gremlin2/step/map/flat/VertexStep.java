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
import org.qubership.itool.modules.gremlin2.Traversal;
import org.qubership.itool.modules.gremlin2.Traverser;
import org.qubership.itool.modules.gremlin2.step.AbstractStep;
import org.qubership.itool.modules.gremlin2.structure.Direction;
import io.vertx.core.json.JsonObject;

import static org.qubership.itool.modules.graph.Graph.F_ID;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

public class VertexStep<E extends JsonObject> extends FlatMapStep<JsonObject, E> {

    private String[] edgeLabels;
    private Direction direction;

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + direction + "," + Arrays.asList(edgeLabels)+ ")";
    }

    public VertexStep(Traversal.Admin traversal, Direction direction, String... edgeLabels) {
        super(traversal);
        this.direction = direction;
        this.edgeLabels = edgeLabels;
    }

    @Override
    protected List<E> flatMap(Traverser.Admin<JsonObject> traverser) {
        List<E> result = new ArrayList<>();

        BasicGraph graph = this.traversal.getGraph();
        JsonObject sourceVertex = requireSourceVertex(traverser);

        // Note: this code produces multiple traverses if there exist multiple edges between this vertex and that one
        switch (this.direction) {
            case OUT:
                fillResult(graph.getSuccessorEdges(sourceVertex.getString(F_ID)), result, edge -> graph.getEdgeTarget(edge.getString(F_ID)));
                break;
            case IN:
                fillResult(graph.getPredecessorEdges(sourceVertex.getString(F_ID)), result, edge -> graph.getEdgeSource(edge.getString(F_ID)));
                break;
            case BOTH:
                fillResult(graph.getSuccessorEdges(sourceVertex.getString(F_ID)), result, edge -> graph.getEdgeTarget(edge.getString(F_ID)));
                fillResult(graph.getPredecessorEdges(sourceVertex.getString(F_ID)), result, edge -> graph.getEdgeSource(edge.getString(F_ID)));
        }
        return result;
    }

    @Override
    protected Traverser<E> generateTraverser(Traverser.Admin<JsonObject> previousTraverser, E value) {
        Traverser<E> traverser = previousTraverser.split(value, value, this);
        return traverser;
    }

    private void fillResult(List<JsonObject> edges, List<E> result, Function<JsonObject, JsonObject> fn) {
        for (JsonObject edge : edges) {
            if (this.edgeLabels.length == 0) {
                JsonObject vertex = fn.apply(edge);
                result.add((E) vertex);
            }
            for (String type : this.edgeLabels) {
                if (type.equals(edge.getString("type"))) {
                    JsonObject vertex = fn.apply(edge);
                    result.add((E) vertex);
                }
            }
        }
    }

    @Override
    public AbstractStep<JsonObject, E> clone() {
        VertexStep clone = (VertexStep) super.clone();
        clone.edgeLabels = this.edgeLabels;
        clone.direction = this.direction;

        return clone;
    }

}
