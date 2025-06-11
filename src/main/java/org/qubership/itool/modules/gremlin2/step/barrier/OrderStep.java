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

package org.qubership.itool.modules.gremlin2.step.barrier;

import org.qubership.itool.modules.gremlin2.Traversal;
import org.qubership.itool.modules.gremlin2.Traverser;
import org.qubership.itool.modules.gremlin2.step.AbstractStep;
import org.qubership.itool.modules.gremlin2.step.ByModulating;
import org.qubership.itool.modules.gremlin2.structure.MapElement;
import org.qubership.itool.modules.gremlin2.util.Order;
import io.vertx.core.json.JsonObject;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class OrderStep<S, E> extends ReducingBarrierStep<S, S> implements ByModulating {

    private Order orderBy = Order.asc;
    private String orderByKey;
    private MapElement orderByMapElement;

    @Override
    public String toString() {
        return getClass().getSimpleName() + "("
            + (orderBy == null ? "": orderBy)
            + (orderByKey == null ? "": orderByKey)
            + (orderByMapElement == null ? "": orderByMapElement)
            + ")";
    }

    public OrderStep(Traversal.Admin traversal) {
        super(traversal);
    }

    @Override
    protected void processAllPreviousTraversers(List<Traverser<S>> previousTraversers, List<Traverser<S>> result) {
        result.addAll(previousTraversers);
        if (this.orderByKey != null) {
            Collections.sort(result, (first, second) -> {
                S firstObj = first == null ? null : first.get();
                S secondObj = second == null ? null : second.get();
                if (firstObj == null && secondObj == null) {
                    return 0;
                }
                if (firstObj == null) {
                    return -1;
                }
                if (secondObj == null) {
                    return 1;
                }

                Object firstValue = null;
                Object secondValue = null;

                if (firstObj instanceof JsonObject) {
                    firstValue = ((JsonObject) firstObj).getValue(orderByKey);
                } else if (firstObj instanceof Map) {
                    firstValue = ((Map)firstObj).get(orderByKey);
                }

                if (secondObj instanceof JsonObject) {
                    secondValue = ((JsonObject) secondObj).getValue(orderByKey);
                } else if (firstObj instanceof Map) {
                    secondValue = ((Map)secondObj).get(orderByKey);
                }

                if (firstValue == null && secondValue == null) {
                    return 0;
                }
                if (firstValue == null) {
                    return -1;
                }
                if (secondValue == null) {
                    return 1;
                }
                return orderBy.compare(firstValue, secondValue);
            });

        } else if(this.orderByMapElement != null) {
            Collections.sort(result, (first, second) -> {
                S firstObj = first == null ? null : first.get();
                S secondObj = second == null ? null : second.get();
                if (firstObj == null && secondObj == null) {
                    return 0;
                }
                if (firstObj == null) {
                    return -1;
                }
                if (secondObj == null) {
                    return 1;
                }

                Object firstValue = null;
                Object secondValue = null;

                if ((firstObj instanceof Map) && (secondObj instanceof Map)) {
                    switch (this.orderByMapElement) {
                    case key:
                        firstValue = ((Map)firstObj).keySet().iterator().next();
                        secondValue = ((Map)secondObj).keySet().iterator().next();
                        break;
                    case value:
                        firstValue = ((Map)firstObj).values().iterator().next();
                        secondValue = ((Map)secondObj).values().iterator().next();
                        break;
                    default:    // No idea what to do
                        break;
                    }
                } else {
                    firstValue = firstObj;
                    secondValue = secondObj;
                }

                if (firstValue == null && secondValue == null) {
                    return 0;
                }
                if (firstValue == null) {
                    return -1;
                }
                if (secondValue == null) {
                    return 1;
                }
                return orderBy.compare(firstValue, secondValue);
            });

        } else {
            Collections.sort(result, this.orderBy);
        }
    }

    @Override
    protected S projectTraversers(List<Traverser<S>> previousTraversers) {
        // do nothing
        return null;
    }

    @Override
    public void modulateBy(Order order) throws UnsupportedOperationException {
        this.orderBy = order;
    }

    @Override
    public void modulateBy(String propertyKey, Order order) throws UnsupportedOperationException {
        this.orderBy = order;
        this.orderByKey = propertyKey;
    }

    @Override
    public void modulateBy(String string) throws UnsupportedOperationException {
        this.orderByKey = string;
    }

    @Override
    public void modulateBy(MapElement mapElement) throws UnsupportedOperationException {
        this.orderByMapElement = mapElement;
    }

    @Override
    public AbstractStep<S, S> clone() {
        OrderStep clone = (OrderStep) super.clone();
        clone.orderBy = this.orderBy;
        return clone;
    }

}
