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

package org.qubership.itool.modules.gremlin2.step.map.scalar;

import org.qubership.itool.modules.gremlin2.Traversal;
import org.qubership.itool.modules.gremlin2.Traverser;
import org.qubership.itool.modules.gremlin2.step.map.MapStep;

import java.util.List;

public abstract class ScalarMapStep<S, E> extends MapStep<S,E> {

    public ScalarMapStep(final Traversal.Admin traversal) {
        super(traversal);
    }

    @Override
    protected void processPreviousTraverser(Traverser.Admin<S> previousTraverser, List<Traverser<E>> result) {
        E currentValue = map(previousTraverser);
        if (currentValue != null) {
            Traverser<E> traverser = generateTraverser(previousTraverser, currentValue);
            result.add(traverser);
        }
    }

    protected Traverser<E> generateTraverser(Traverser.Admin<S> previousTraverser, E value) {
        Traverser<E> traverser = previousTraverser.split(value, this);
        return traverser;
    }

    protected abstract E map(final Traverser.Admin<S> traverser);

}
