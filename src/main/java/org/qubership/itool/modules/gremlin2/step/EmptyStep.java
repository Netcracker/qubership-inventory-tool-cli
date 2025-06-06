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

package org.qubership.itool.modules.gremlin2.step;

import org.qubership.itool.modules.gremlin2.Step;
import org.qubership.itool.modules.gremlin2.Traversal;
import org.qubership.itool.modules.gremlin2.Traverser;
import org.qubership.itool.modules.gremlin2.util.EmptyTraversal;

import java.util.*;

public class EmptyStep<S, E> implements Step<S, E> {
    private static final EmptyStep INSTANCE = new EmptyStep<>();

    private EmptyStep() {
    }

    public static <S, E> EmptyStep<S, E> getInstance() {
        return INSTANCE;
    }

    @Override
    public void setPreviousStep(Step<?, S> step) {
    }

    @Override
    public Step<?, S> getPreviousStep() {
        return INSTANCE;
    }

    @Override
    public void setNextStep(Step<E, ?> step) {
    }

    @Override
    public Step<E, ?> getNextStep() {
        return INSTANCE;
    }

    @Override
    public void setTraversal(Traversal.Admin<?, ?> traversal) {
        // do nothing
    }

    @Override
    public <A, B> Traversal.Admin<A, B> getTraversal() {
        return EmptyTraversal.getInstance();
    }

    @Override
    public Set<String> getLabels() {
        return null;
    }

    @Override
    public void addLabel(String label) {
        // do nothing
    }

    @Override
    public void removeLabel(String label) {
        // do nothing
    }

    @Override
    @SuppressWarnings("CloneDoesntCallSuperClone")
    public EmptyStep<S, E> clone() {
        return INSTANCE;
    }

    @Override
    public List<Traverser<E>> getTraversers() {
        return new ArrayList<>();
    }

    @Override
    public int hashCode() {
        return -1691648055;
    }

    @Override
    public boolean equals(final Object object) {
        return object instanceof EmptyStep;
    }

}
