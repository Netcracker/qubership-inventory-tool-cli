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

import org.qubership.itool.modules.gremlin2.Traversal;
import org.qubership.itool.modules.gremlin2.Traverser;
import org.qubership.itool.modules.gremlin2.step.AbstractStep;

import java.util.List;

public class UnionStep<S, E> extends AbstractStep<S, E> {

    private Traversal.Admin<?, E>[] unionTraversals;

    public UnionStep(Traversal.Admin traversal, Traversal.Admin<?, E>  ... unionTraversals) {
        super(traversal);
        this.unionTraversals = unionTraversals;
    }

    @Override
    public void clear() {
        if (unionTraversals != null) {
            for (Traversal.Admin traversal : unionTraversals) {
                traversal.clear();
            }
        }
    }

    @Override
    protected void processPreviousTraverser(Traverser.Admin<S> previousTraverser, List<Traverser<E>> result) {
        for (Traversal.Admin<?, E> unionTraversal : this.unionTraversals) {
            Traversal.Admin<?, E> cloneTraversal = prepareInnerTraversal(unionTraversal, previousTraverser);
            List<Traverser<E>> cloneResultList = cloneTraversal.getEndStep().getTraversers();
            for (Traverser<E> cloneResult : cloneResultList) {
                result.add(cloneResult);
            }
        }
    }

    @Override
    public AbstractStep<S, E> clone() {
        UnionStep clone = (UnionStep) super.clone();
        clone.unionTraversals = this.unionTraversals;
        return clone;
    }

}
