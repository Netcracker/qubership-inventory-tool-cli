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

import org.qubership.itool.modules.gremlin2.P;
import org.qubership.itool.modules.gremlin2.Traversal;
import org.qubership.itool.modules.gremlin2.Traverser;
import org.qubership.itool.modules.gremlin2.step.AbstractStep;
import org.qubership.itool.modules.gremlin2.step.ByModulating;

import java.util.Optional;

public class WherePredicateStep<S> extends FilterStep<S> implements ByModulating {

    protected String startKey;
    protected P<Object> predicate;
    protected Traversal.Admin<?, ?> innerTraversal;

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + startKey + "," + predicate + ((innerTraversal == null) ? "" : ",traversal") + ")";
    }

    @Override
    public void clear() {
        clearTraversal(innerTraversal);
    }

    public WherePredicateStep(Traversal.Admin traversal, Optional<String> startKey, P<String> predicate) {
        super(traversal);
        this.startKey = startKey.orElse(null);
        this.predicate = (P) predicate;
    }

    public WherePredicateStep(Traversal.Admin traversal, Traversal<?, ?> innerTraversal) {
        super(traversal);
        this.innerTraversal = innerTraversal.asAdmin();
    }

    @Override
    protected boolean filter(Traverser.Admin<S> traverser) {
        if (this.predicate != null) {
            return filterByPredicate(traverser);
        }
        if (this.innerTraversal != null) {
            return filterByTraversal(traverser);
        }
        throw new UnsupportedOperationException(this.getClass().getSimpleName() + " required predicate or traversal");
    }

    private boolean filterByTraversal(Traverser.Admin<S> traverser) {
        Traversal.Admin<?, ?> cloneTraversal = prepareInnerTraversal((Traversal.Admin<?, S>) this.innerTraversal, traverser);
        Object result = cloneTraversal.next();
        return (result != null);
    }

    private boolean filterByPredicate(Traverser.Admin<S> traverser) {
        P<Object> clonePredicate = this.predicate.clone();
        if (startKey == null) {
            String origPredicateValue = (String)clonePredicate.getValue();
            Object predicateObject = traverser.path().get(origPredicateValue);
            clonePredicate.setOriginalValue(predicateObject);
        }
        return clonePredicate.test(traverser.get());
    }

    @Override
    public AbstractStep<S, S> clone() {
        WherePredicateStep clone = (WherePredicateStep) super.clone();
        clone.startKey = this.startKey;
        clone.predicate = (this.predicate != null) ? this.predicate.clone() : null;
        clone.innerTraversal = (this.innerTraversal != null) ? this.innerTraversal.clone() : null;
        return clone;
    }

}
