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
import java.util.List;

public abstract class FlatMapStep<S, E> extends AbstractStep<S, E> {

    public FlatMapStep(final Traversal.Admin traversal) {
        super(traversal);
    }

    @Override
    @SuppressWarnings("CloneDoesntDeclareCloneNotSupportedException")
    public AbstractStep<S, E> clone() {
        final FlatMapStep<S, E> clone = (FlatMapStep<S, E>) super.clone();
        return clone;
    }

    @Override
    protected void processPreviousTraverser(Traverser.Admin<S> previousTraverser, List<Traverser<E>> result) {
        result.addAll(flatMapTraverser(previousTraverser));
    }

    protected Traverser<E> generateTraverser(Traverser.Admin<S> previousTraverser, E value) {
        Traverser<E> traverser = previousTraverser.split(value, this);
        return traverser;
    }

    protected List<Traverser<E>> flatMapTraverser(final Traverser.Admin<S> traverser) {
        List<Traverser<E>> result = new ArrayList<>();
        for (E obj : flatMap(traverser)) {
            Traverser<E> tmp = generateTraverser(traverser, obj);
            result.add(tmp);
        }
        return result;
    }

    protected abstract List<E> flatMap(final Traverser.Admin<S> traverser);

}
