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

package org.qubership.itool.modules.gremlin2.step;

import org.qubership.itool.modules.gremlin2.Traversal;
import org.qubership.itool.modules.gremlin2.Traverser;
import org.qubership.itool.modules.graph.BasicGraph;
import org.qubership.itool.modules.gremlin2.DefaultTraverser;
import org.qubership.itool.modules.gremlin2.util.ElementType;

import io.vertx.core.json.JsonObject;

import java.util.*;
import java.util.function.Supplier;

public class GraphStep<S, E extends JsonObject> extends AbstractStep<S, E> {

    protected String[] ids;
    protected boolean isStart;
    protected Class<E> returnClass;
    protected Supplier<Iterator<S>> iteratorSupplier;
    protected ElementType elementType;

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + elementType + "," + Arrays.toString(ids) + ")";
    }

    @SuppressWarnings("unchecked")
    public GraphStep(Traversal.Admin traversal, Class<E> returnClass, boolean isStart, ElementType elementType, String ... ids) {
        super(traversal);
        this.returnClass = returnClass;
        this.isStart = isStart;
        this.elementType = elementType;
        this.ids = ids;

        switch (this.elementType){
            case vertex:
                this.iteratorSupplier = () -> (ids.length == 0) ?
                      (Iterator<S>)getTraversal().getGraph().vertexList().iterator()
                    : (Iterator<S>)getVerticesByIds().iterator();
                break;
            case edge:
                this.iteratorSupplier = () -> (ids.length == 0) ?
                      (Iterator<S>)getTraversal().getGraph().edgeList().iterator()
                    : (Iterator<S>)getEdgesByIds().iterator();
        }
    }

    private List<JsonObject> getVerticesByIds() {
        BasicGraph g = getTraversal().getGraph();
        List<JsonObject> result = new ArrayList<>(ids.length);
        for (String id: ids) {
            JsonObject v = g.getVertex(id);
            if (v != null) {
                result.add(v);
            }
        }
        return result;
    }

    private List<JsonObject> getEdgesByIds() {
        BasicGraph g = getTraversal().getGraph();
        List<JsonObject> result = new ArrayList<>(ids.length);
        for (String id: ids) {
            JsonObject e = g.getEdge(id);
            if (e != null) {
                result.add(e);
            }
        }
        return result;
    }

    @Override
    protected List<Traverser<S>> fetchPreviousTraversers() {
        List<Traverser<S>> result = new ArrayList<>();
        Iterator<S> iterator = this.iteratorSupplier.get();
        while (iterator.hasNext()) {
            S item = iterator.next();
            Traverser<S> traverser = new DefaultTraverser<S>((JsonObject)item, item);
            result.add(traverser);
        }
        return result;
    }

    @Override
    protected void processPreviousTraverser(Traverser.Admin<S> previousTraverser, List<Traverser<E>> result) {
        result.add((Traverser<E>) previousTraverser);
    }

    @Override
    public GraphStep<S, E> clone() {
        GraphStep<S, E> clone = (GraphStep) super.clone();
        clone.isStart = isStart;
        clone.returnClass = this.returnClass;
        clone.elementType = this.elementType;
        clone.ids = this.ids;
//        clone.iteratorSupplier = this.iteratorSupplier;
        return clone;
    }

}
