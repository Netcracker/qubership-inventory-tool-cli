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

import org.qubership.itool.modules.gremlin2.Traversal;
import org.qubership.itool.modules.gremlin2.Traverser;
import org.qubership.itool.modules.gremlin2.step.AbstractStep;
import org.qubership.itool.modules.gremlin2.step.ByModulating;
import org.qubership.itool.modules.gremlin2.structure.MapElement;
import io.vertx.core.json.JsonArray;

import java.util.*;

public class UnfoldStep<S, E> extends FlatMapStep<S, E> implements ByModulating {

    private String modulateByProperty;
    private MapElement modulateBy;

    @Override
    public String toString() {
        return getClass().getSimpleName() + "("
            + (modulateByProperty == null ? "": modulateByProperty)
            + (modulateBy == null ? "": modulateBy)
            + ")";
    }

    public UnfoldStep(Traversal.Admin traversal) {
        super(traversal);
        this.modulateBy = MapElement.value;
    }

    @Override
    protected List<E> flatMap(Traverser.Admin<S> traverser) {
        S source = traverser.get();
        List<E> result = new ArrayList<>();

        if (source instanceof Map) {
            Map map = (Map)source;
            if (this.modulateByProperty != null) {
                Object tmp = map.get(this.modulateByProperty);
                if (tmp == null) {
                    return result;
                }

                List list;
                if (tmp instanceof List) {
                    list = (List)tmp;
                } else if (tmp instanceof JsonArray) {
                    list = ((JsonArray)tmp).getList();
                } else {
                    throw new UnsupportedOperationException("UnfoldStep with .by(String) support only List or JsonArray property");
                }

                for (Object obj : list) {
                    Map clone = new HashMap();
                    for (Object key : map.keySet()) {
                        clone.put(key, map.get(key));
                    }
                    clone.put(this.modulateByProperty, obj);
                    result.add((E)clone);
                }
                return result;

            } else {
                switch (this.modulateBy) {
                    case key:
                        for (Object key : map.keySet()) {
                            source = (S)key;
                            processTraverserObject(source, result);
                        }
                        break;
                    case value:
                        for (Object value : map.values()) {
                            source = (S)value;
                            processTraverserObject(source, result);
                        }
                        break;
                    case both:
                        for (Object key : map.keySet()) {
                            Map tmpMap = new HashMap();
                            tmpMap.put(key, map.get(key));
                            result.add((E) tmpMap);
//                            processTraverserObject((S)key, result);
//                            processTraverserObject((S)map.get(key), result);
                        }
                }
            }

        } else {
            processTraverserObject(source, result);
        }
        return result;
    }

    private void processTraverserObject(S source, List<E> result) {
        if (source instanceof List) {
            List<S> sourceList = (List)source;
            for (S item : sourceList) {
                if (item != null) {
                    result.add((E)item);
                }
            }

        } else if (source instanceof JsonArray) {
            JsonArray jsonArray = (JsonArray) source;
            for (Object item : jsonArray.getList()) {
                if (item != null) {
                    result.add((E)item);
                }
            }

        } else {
            if (source != null) {
                result.add((E)source);
            }
        }
    }

    @Override
    protected Traverser<E> generateTraverser(Traverser.Admin<S> previousTraverser, E value) {
        Traverser<E> traverser = previousTraverser.split(
            (previousTraverser.getSource() != null) ? previousTraverser.getSource() : null,
            value, this);
        return traverser;
    }

    @Override
    public void modulateBy(String string) throws UnsupportedOperationException {
        this.modulateByProperty = string;
    }

    @Override
    public void modulateBy(MapElement mapElement) throws UnsupportedOperationException {
        this.modulateBy = mapElement;
    }

    @Override
    public AbstractStep<S, E> clone() {
        UnfoldStep clone = (UnfoldStep) super.clone();
        clone.modulateByProperty = this.modulateByProperty;
        clone.modulateBy = this.modulateBy;
        return clone;
    }

}
