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

package org.qubership.itool.modules.gremlin2.step.filter;

import org.qubership.itool.modules.gremlin2.Traversal;
import org.qubership.itool.modules.gremlin2.Traverser;
import org.qubership.itool.modules.gremlin2.step.AbstractStep;
import org.qubership.itool.modules.gremlin2.step.util.HasContainer;
import org.qubership.itool.modules.gremlin2.step.util.HasContainerHolder;
import org.qubership.itool.modules.gremlin2.util.TraversalHelper;
import org.qubership.itool.modules.gremlin2.util.ValueHelper;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class HasStep<S> extends FilterStep<S> implements HasContainerHolder {

    private String type;
    private List<HasContainer> hasContainers;

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + (type == null ? "" : (type + ","))  + hasContainers + ")";
    }

    public HasStep(Traversal.Admin traversal, HasContainer hasContainer) {
        super(traversal);
        this.hasContainers = new ArrayList<>();
        this.hasContainers.add(hasContainer);
    }

    public HasStep(Traversal.Admin traversal, String type, HasContainer hasContainer) {
        super(traversal);
        this.type = type;
        this.hasContainers = new ArrayList<>();
        this.hasContainers.add(hasContainer);
    }

    @Override
    protected boolean filter(Traverser.Admin<S> traverser) {
        if (this.type != null && traverser.get() instanceof JsonObject) {
            JsonObject json = (JsonObject) traverser.get();
            if (!this.type.equals(json.getString("type"))) {
                return false;
            }
        }

        boolean result = true;
        for (HasContainer hasContainer : this.hasContainers) {
            S obj = traverser.get();
            Object value = ValueHelper.getObjectValue(hasContainer.getPropertyKey(), obj);
            Object predicateValue = hasContainer.getPredicate().getValue();
            if (predicateValue instanceof Traversal) {
                Traversal.Admin predicateTraversal = ((Traversal.Admin<?, ?>) predicateValue).clone();
                TraversalHelper.propagateSource(this.traversal, predicateTraversal);
                predicateTraversal.setRoot(true);
                predicateTraversal.addStart((Traverser.Admin) traverser.clone());
                predicateValue = predicateTraversal.next();
                hasContainer.getPredicate().setOriginalValue(predicateValue);
            }

            if (!hasContainer.test(value)) {
                result = false;
                break;
            }
            hasContainer.getPredicate().setOriginalValue(null);
        }
        return result;
    }

    @Override
    public AbstractStep<S, S> clone() {
        HasStep clone = (HasStep)super.clone();
        clone.type = this.type;
        clone.hasContainers = new ArrayList();
        for (HasContainer hasContainer : this.hasContainers) {
            clone.hasContainers.add(hasContainer);
        }
        return clone;
    }

    @Override
    public List<HasContainer> getHasContainers() {
        return this.hasContainers;
    }

    @Override
    public void addHasContainer(HasContainer hasContainer) {
        if (this.hasContainers == null) {
            this.hasContainers = new ArrayList<>();
        }
        this.hasContainers.add(hasContainer);
    }

}
