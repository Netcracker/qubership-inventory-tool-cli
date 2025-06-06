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

public class IsStep<S> extends FilterStep<S> {

    private P<S> predicate;

    @Override
    public String toString() {
        return super.toString() + predicate.toString();
    }

    public IsStep(final Traversal.Admin traversal, final P<S> predicate) {
        super(traversal);
        this.predicate = predicate;
    }

    @Override
    protected boolean filter(Traverser.Admin<S> traverser) {
        return this.predicate.test(traverser.get());
    }

    @Override
    public IsStep<S> clone() {
        IsStep<S> clone = (IsStep<S>) super.clone();
        clone.predicate = this.predicate.clone();
        return clone;
    }

}
