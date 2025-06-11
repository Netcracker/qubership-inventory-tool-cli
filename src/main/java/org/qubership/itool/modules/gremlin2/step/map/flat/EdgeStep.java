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

import static org.qubership.itool.modules.graph.Graph.F_ID;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EdgeStep<E extends JsonObject> extends FlatMapStep<JsonObject, E> {

    private String[] edgeLabels;
    private Direction direction;

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + direction + "," + Arrays.asList(edgeLabels)+ ")";
    }

    public EdgeStep(Traversal.Admin traversal, Direction direction, String... edgeLabels) {
        super(traversal);
        this.direction = direction;
        this.edgeLabels = edgeLabels;
    }

    @Override
    protected Traverser<E> generateTraverser(Traverser.Admin<JsonObject> previousTraverser, E value) {
        Traverser<E> traverser = previousTraverser.split(value, value, this);
        return traverser;
    }

    @Override
    protected List<E> flatMap(Traverser.Admin<JsonObject> traverser) {
        List<E> result = new ArrayList<>();
        BasicGraph graph = this.traversal.getGraph();

        JsonObject sourceVertex = null;
        if (graph != null) {
            sourceVertex = traverser.getSource();
            if (sourceVertex == null) {
                sourceVertex = traverser.get();
            }
        }
        if (sourceVertex == null || graph.getVertex(sourceVertex.getString(F_ID)) != sourceVertex) {
            throw new GremlinException("Step " + this.getClass().getSimpleName() + " applicable only to Vertex");
        }

        switch (this.direction) {
            case OUT:
                fillResult(graph.getSuccessorEdges(sourceVertex.getString("id")), result);
                break;
            case IN:
                fillResult(graph.getPredecessorEdges(sourceVertex.getString("id")), result);
                break;
            case BOTH:
                fillResult(graph.getSuccessorEdges(sourceVertex.getString("id")), result);
                fillResult(graph.getPredecessorEdges(sourceVertex.getString("id")), result);
        }
        return result;
    }

    private void fillResult(List<JsonObject> edges, List<E> result) {
        for (JsonObject edge : edges) {
            if (this.edgeLabels.length == 0) {
                result.add((E) edge);
            } else {
                String edgeType = edge.getString("type");
                for (String type : this.edgeLabels) {
                    if (type.equals(edgeType)) {
                        result.add((E) edge);
                    }
                }
            }
        }
    }

    @Override
    public AbstractStep<JsonObject, E> clone() {
        EdgeStep clone = (EdgeStep)super.clone();
        clone.edgeLabels = this.edgeLabels;
        clone.direction = this.direction;
        return clone;
    }
}
