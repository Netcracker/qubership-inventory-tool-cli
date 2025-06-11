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
import org.qubership.itool.modules.gremlin2.util.TraversalHelper;

import java.util.ArrayList;
import java.util.List;

public class OrStep<S> extends FilterStep<S> {

    private List<Traversal.Admin<?, S>> orTraversals;

    public OrStep(Traversal.Admin traversal, Traversal.Admin<?, S>  ... orTraversals) {
        super(traversal);
        this.orTraversals = new ArrayList<>();
        for (Traversal.Admin<?, S> orTraversal : orTraversals) {
            this.orTraversals.add(orTraversal);
        }
    }

    @Override
    public void clear() {
        if (orTraversals != null) {
            for (Traversal.Admin traversal : orTraversals) {
                clearTraversal(traversal);
            }
        }
    }

    @Override
    protected boolean filter(Traverser.Admin<S> traverser) {
        boolean founded = false;
        for (Traversal.Admin<?, S> orTraversal: this.orTraversals) {
            TraversalHelper.propagateSource(this.traversal, orTraversal);
            Traversal.Admin<?, S> cloneTraversal = prepareInnerTraversal(orTraversal, traverser);
            List<Traverser<S>> cloneResultList = cloneTraversal.getEndStep().getTraversers();
            if (cloneResultList.size() != 0) {
                founded = true;
                break;
            }
        }
        return founded;
    }

    @Override
    public AbstractStep<S, S> clone() {
        OrStep clone = (OrStep) super.clone();
        clone.orTraversals = new ArrayList();
        for (Traversal.Admin<?, S> traversal : this.orTraversals) {
            clone.orTraversals.add(traversal);
        }
        return clone;
    }

    public void appendOrTraversals(Traversal.Admin<?, S> traversal) {
        this.orTraversals.add(traversal);
    }

}
