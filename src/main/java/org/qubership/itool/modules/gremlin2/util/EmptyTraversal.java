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

package org.qubership.itool.modules.gremlin2.util;

import org.qubership.itool.modules.graph.BasicGraph;
import org.qubership.itool.modules.gremlin2.Step;
import org.qubership.itool.modules.gremlin2.Traversal;
import org.qubership.itool.modules.gremlin2.Traverser;

import java.util.Collections;
import java.util.List;

public class EmptyTraversal<S, E> implements Traversal.Admin<S, E> {

    private static final EmptyTraversal INSTANCE = new EmptyTraversal();

    protected EmptyTraversal() {
    }

    public static <A, B> EmptyTraversal<A, B> getInstance() {
        return INSTANCE;
    }

    @Override
    public Traversal.Admin<S, E> asAdmin() {
        return this;
    }

    @Override
    public List<Step> getSteps() {
        return Collections.emptyList();
    }

    @Override
    public <S2, E2> Admin<S2, E2> addStep(int index, Step<?, ?> step) throws IllegalStateException {
        return getInstance();
    }

    @Override
    public <S2, E2> Admin<S2, E2> replaceStep(int index, Step<?, ?> step) throws IllegalStateException {
        return getInstance();
    }

    @Override
    public Admin<S, E> clone() {
        return getInstance();
    }

    @Override
    public Admin<S, E> getPreviousTraversal() {
        return null;
    }

    @Override
    public void setPreviousTraversal(Admin<S, E> traversal) {
        // do nothing
    }

    @Override
    public BasicGraph getGraph() {
        return null;
    }

    @Override
    public void setGraph(BasicGraph graph) {
        // do nothing
    }

    @Override
    public void applyStrategies() throws IllegalStateException {
        // do nothing
    }

    @Override
    public boolean isLocked() {
        return false;
    }

    @Override
    public boolean isRoot() {
        return false;
    }

    @Override
    public void setRoot(boolean isRoot) {
        // do nothing
    }

    @Override
    public void addStart(Traverser.Admin<S> innerTraverser) {
        // do nothing
    }

    @Override
    public boolean equals(final Object object) {
        return object instanceof EmptyTraversal;
    }

    @Override
    public int hashCode() {
        return -343564565;
    }

}
