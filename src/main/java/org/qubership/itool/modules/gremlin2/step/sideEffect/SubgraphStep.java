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

package org.qubership.itool.modules.gremlin2.step.sideEffect;

import org.qubership.itool.modules.graph.BasicGraph;
import org.qubership.itool.modules.graph.SubGraph;
import org.qubership.itool.modules.gremlin2.GremlinException;
import org.qubership.itool.modules.gremlin2.Traversal;
import org.qubership.itool.modules.gremlin2.Traverser;
import org.qubership.itool.modules.gremlin2.step.AbstractStep;
import io.vertx.core.json.JsonObject;

import static org.qubership.itool.modules.graph.Graph.F_ID;

import java.util.ArrayList;

public class SubgraphStep extends SideEffectStep<JsonObject> {

    protected String sideEffectKey;

    public SubgraphStep(Traversal.Admin traversal, String sideEffectKey) {
        super(traversal);
        this.sideEffectKey = sideEffectKey;
    }

    @Override
    protected void sideEffect(Traverser.Admin<JsonObject> traverser) {
        Traversal.Admin rootTraversal = fetchRootTraversal();
        BasicGraph graph = this.traversal.getGraph();
        SubGraph subGraph = (SubGraph) rootTraversal.getSideEffect(this.sideEffectKey);
        if (subGraph == null) {
            subGraph = new SubGraph(graph, new ArrayList<>());
            rootTraversal.addSideEffect(this.sideEffectKey, subGraph);
        }
        JsonObject edge = traverser.get();
        if (edge == null || graph.getEdge(edge.getString(F_ID)) != edge) {
            throw new GremlinException(this.getClass().getSimpleName() + " applicable only to Edge");
        }
        subGraph.addEdge(edge.getString(F_ID));
    }

    @Override
    public AbstractStep<JsonObject, JsonObject> clone() {
        SubgraphStep clone = (SubgraphStep) super.clone();
        clone.sideEffectKey = this.sideEffectKey;
        return clone;
    }

}
