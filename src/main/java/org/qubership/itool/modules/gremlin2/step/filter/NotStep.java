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

public class NotStep<S> extends FilterStep<S> {

    private Traversal.Admin<?, ?> innerTraversal;

    public NotStep(Traversal.Admin traversal, Traversal<?, ?> innerTraversal) {
        super(traversal);
        this.innerTraversal = innerTraversal.asAdmin();
    }

    @Override
    public void clear() {
        clearTraversal(innerTraversal);
    }

    @Override
    protected boolean filter(Traverser.Admin<S> traverser) {
        Traversal.Admin<?, ?> cloneTraversal = prepareInnerTraversal((Traversal.Admin<?, S>) this.innerTraversal, traverser);
        Object result = cloneTraversal.next();
        return (result == null);
    }

    @Override
    public AbstractStep<S, S> clone() {
        NotStep clone = (NotStep) super.clone();
        clone.innerTraversal = this.innerTraversal.clone();
        return clone;
    }

}
