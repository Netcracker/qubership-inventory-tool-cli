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

package org.qubership.itool.modules.gremlin2.graph.lambda;

import org.qubership.itool.modules.graph.BasicGraph;
import org.qubership.itool.modules.gremlin2.Step;
import org.qubership.itool.modules.gremlin2.Traversal;
import org.qubership.itool.modules.gremlin2.Traverser;

import java.util.Collections;
import java.util.List;

public abstract class AbstractLambdaTraversal<S, E> implements Traversal.Admin<S, E>  {

    protected Traversal.Admin<S, E> bypassTraversal = null;

    public void setBypassTraversal(final Traversal.Admin<S, E> bypassTraversal) {
        this.bypassTraversal = bypassTraversal;
    }

    @Override
    public List<Step> getSteps() {
        return null == this.bypassTraversal ? Collections.emptyList() : this.bypassTraversal.getSteps();
    }

    @Override
    public <S2, E2> Admin<S2, E2> addStep(int index, Step<?, ?> step) throws IllegalStateException {
        return null == this.bypassTraversal ?
            (Traversal.Admin<S2, E2>) this : this.bypassTraversal.addStep(index, step);
    }

    @Override
    public <S2, E2> Admin<S2, E2> replaceStep(int index, Step<?, ?> step) throws IllegalStateException {
        return null == this.bypassTraversal ?
            (Traversal.Admin<S2, E2>) this : this.bypassTraversal.replaceStep(index, step);
    }

    @Override
    public Admin<S, E> clone() {
        try {
            final AbstractLambdaTraversal<S, E> clone = (AbstractLambdaTraversal<S, E>) super.clone();
            if (null != this.bypassTraversal)
                clone.bypassTraversal = this.bypassTraversal.clone();
            return clone;
        } catch (final CloneNotSupportedException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    @Override
    public Admin<S, E> getPreviousTraversal() {
        return this.bypassTraversal == null ? null : this.bypassTraversal.getPreviousTraversal();
    }

    @Override
    public void setPreviousTraversal(Admin<S, E> traversal) {
        if (this.bypassTraversal != null) {
            this.bypassTraversal.setPreviousTraversal(traversal);
        }

    }

    @Override
    public BasicGraph getGraph() {
        return this.bypassTraversal == null ? null : this.bypassTraversal.getGraph();
    }

    @Override
    public void setGraph(BasicGraph graph) {
        if (this.bypassTraversal != null) {
            this.bypassTraversal.setGraph(graph);
        }
    }

    @Override
    public void applyStrategies() throws IllegalStateException {
        if (this.bypassTraversal != null) {
            this.bypassTraversal.applyStrategies();
        }
    }

    @Override
    public boolean isLocked() {
        return null == this.bypassTraversal || this.bypassTraversal.isLocked();
    }

    @Override
    public boolean isRoot() {
        return null == this.bypassTraversal || this.bypassTraversal.isRoot();
    }

    @Override
    public void setRoot(boolean isRoot) {
        if (this.bypassTraversal != null) {
            this.bypassTraversal.setRoot(isRoot);
        }
    }

    @Override
    public void addStart(Traverser.Admin<S> innerTraverser) {
        if (null != this.bypassTraversal)
            this.bypassTraversal.addStart(innerTraverser);
    }

}
