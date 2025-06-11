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

package org.qubership.itool.modules.gremlin2.step.map;


import org.qubership.itool.modules.gremlin2.DefaultTraverser;
import org.qubership.itool.modules.gremlin2.Traversal;
import org.qubership.itool.modules.gremlin2.Traverser;
import org.qubership.itool.modules.gremlin2.step.AbstractStep;

import java.util.List;

public class CapStep<S, E> extends MapStep<S, E> {

    private String sideEffectKey;

    public CapStep(Traversal.Admin traversal, String sideEffectKey) {
        super(traversal);
        this.sideEffectKey = sideEffectKey;
    }

    @Override
    protected void processPreviousTraverser(Traverser.Admin<S> previousTraverser, List<Traverser<E>> result) {
        Traversal.Admin rootTraversal = fetchRootTraversal();
        Object obj = rootTraversal.getSideEffect(this.sideEffectKey);
        Traverser<E> traverser = new DefaultTraverser(obj);
        result.add(traverser);
    }

    @Override
    public AbstractStep<S, E> clone() {
        CapStep clone = (CapStep) super.clone();
        clone.sideEffectKey = this.sideEffectKey;
        return clone;
    }

}
