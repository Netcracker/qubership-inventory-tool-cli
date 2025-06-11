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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CoalesceStep<S, E> extends FlatMapStep<S, E> {

    private List<Traversal.Admin<S, E>> coalesceTraversals;

    public CoalesceStep(Traversal.Admin<S, E> traversal, Traversal.Admin<S, E> ... coalesceTraversals) {
        super(traversal);
        this.coalesceTraversals = Arrays.asList(coalesceTraversals);
    }

    @Override
    protected List<Traverser<E>> flatMapTraverser(Traverser.Admin<S> traverser) {
        for (Traversal.Admin<S, E> coalesceTraversal : this.coalesceTraversals) {
            Traverser.Admin<S> innerTraverser = traverser.clone().asAdmin();
            Traversal.Admin<S, E> cloneTraversal = coalesceTraversal.clone();
            cloneTraversal.setRoot(true);
            cloneTraversal.setGraph(this.traversal.getGraph());
            cloneTraversal.addStart(innerTraverser);
            List<Traverser<E>> result = cloneTraversal.getEndStep().getTraversers();
            if (result.size() != 0) {
                return result;
            }
        }
        return new ArrayList<>();
    }

    @Override
    protected List<E> flatMap(Traverser.Admin<S> traverser) {
        // do nothing
        throw new IllegalStateException("Impossible situation");
    }

    @Override
    public AbstractStep<S, E> clone() {
        CoalesceStep clone = (CoalesceStep) super.clone();
        clone.coalesceTraversals = new ArrayList<>();
        for (final Traversal.Admin<S, E> conjunctionTraversal : this.coalesceTraversals) {
            clone.coalesceTraversals.add(conjunctionTraversal.clone());
        }
        return clone;
    }

}
