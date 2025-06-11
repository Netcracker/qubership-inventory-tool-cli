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

package org.qubership.itool.modules.gremlin2.step.map.scalar;

import org.qubership.itool.modules.graph.BasicGraph;
import org.qubership.itool.modules.gremlin2.Path;
import org.qubership.itool.modules.gremlin2.Traversal;
import org.qubership.itool.modules.gremlin2.Traverser;
import org.qubership.itool.modules.gremlin2.step.AbstractStep;

import io.vertx.core.json.JsonObject;

import static org.qubership.itool.modules.graph.Graph.F_ID;

public class SelectScalarStep<S, E> extends ScalarMapStep<S, E> {

    private String label;

    @Override
    public String toString() {
        return super.toString() + ((label == null) ? "" : "(" + label + ")");
    }

    public SelectScalarStep(Traversal.Admin traversal, String label) {
        super(traversal);
        this.label = label;
    }

    @Override
    protected E map(Traverser.Admin<S> previousTraverser) {
        Path path = previousTraverser.path();
        return path.get(label);
    }

    @Override
    public AbstractStep<S, E> clone() {
        SelectScalarStep clone = (SelectScalarStep) super.clone();
        clone.label = this.label;
        return clone;
    }

    @Override
    protected Traverser<E> generateTraverser(Traverser.Admin<S> previousTraverser, E value) {
        BasicGraph graph = traversal.getGraph();
        // select() changes traverser source
        JsonObject newSource = null;
        if (graph != null && value instanceof JsonObject) {
            JsonObject asJson = (JsonObject) value;
            String id = asJson.getString(F_ID);
            if (graph.getVertex(id) == asJson || graph.getEdge(id) == asJson) {
                newSource = asJson;
            }
        }
        Traverser<E> traverser = previousTraverser.split(newSource, value, this);
        return traverser;
    }

}
