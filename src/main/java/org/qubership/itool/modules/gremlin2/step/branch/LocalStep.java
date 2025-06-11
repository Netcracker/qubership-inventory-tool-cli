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

package org.qubership.itool.modules.gremlin2.step.branch;

import org.qubership.itool.modules.gremlin2.Step;
import org.qubership.itool.modules.gremlin2.Traversal;
import org.qubership.itool.modules.gremlin2.Traverser;
import org.qubership.itool.modules.gremlin2.step.AbstractStep;

import java.util.List;

public class LocalStep<S, E> extends AbstractStep<S, E> {

    protected Traversal.Admin<?, ?> innerTraversal;

    public <E2> LocalStep(Traversal.Admin<S, E> traversal, Traversal.Admin<?, E2> innerTraversal) {
        super(traversal);
        this.innerTraversal = innerTraversal;
    }

    @Override
    public void clear() {
        clearTraversal(innerTraversal);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    protected void processPreviousTraverser(Traverser.Admin<S> previousTraverser, List<Traverser<E>> result) {
        Traversal.Admin<?, E> cloneTraversal = prepareInnerTraversal((Traversal.Admin<?, E>) innerTraversal, previousTraverser);
        List<Traverser<E>> cloneResultList = cloneTraversal.getEndStep().getTraversers();

        for (Traverser<E> cloneResult : cloneResultList) {
            Traverser<E> newTraverser = previousTraverser.split(
                previousTraverser.getSource(), cloneResult.get(), (Step)cloneTraversal.getEndStep());
            result.add(newTraverser);
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public AbstractStep<S, E> clone() {
        LocalStep clone = (LocalStep) super.clone();
        clone.innerTraversal = this.innerTraversal.clone();
        return clone;
    }

}
