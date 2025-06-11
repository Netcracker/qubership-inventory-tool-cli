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

package org.qubership.itool.modules.gremlin2.graph;

import org.qubership.itool.modules.graph.BasicGraph;
import org.qubership.itool.modules.gremlin2.Step;
import org.qubership.itool.modules.gremlin2.Traversal;
import org.qubership.itool.modules.gremlin2.Traverser;
import org.qubership.itool.modules.gremlin2.step.EmptyStep;
import org.qubership.itool.modules.gremlin2.step.StartStep;
import org.qubership.itool.modules.gremlin2.step.util.DefaultProfile;
import org.qubership.itool.modules.gremlin2.step.util.Profile;
import org.qubership.itool.modules.gremlin2.util.TraversalHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultGraphTraversal<S, E> implements GraphTraversal<S, E>, GraphTraversal.Admin<S, E> {

    protected Profile profile = new DefaultProfile();
    protected Map<String, Object> sideEffectMap = new HashMap<>();
    protected Traversal.Admin<S, E> previousTraversal;
    protected GraphTraversalSource graphTraversalSource;
    protected BasicGraph graph;
    protected List<Step> steps = new ArrayList<>();
    protected boolean locked = false;
    boolean root = false;

    public DefaultGraphTraversal() {
    }

    public DefaultGraphTraversal(GraphTraversalSource graphTraversalSource, boolean root) {
        this.root = root;
        setSource(graphTraversalSource);
    }

    @Override
    public Profile getProfile() {
        return this.profile;
    }

    @Override
    public GraphTraversal.Admin<S, E> asAdmin() {
        return this;
    }

    @Override
    public List<Step> getSteps() {
        return this.steps;
    }

    @Override
    public <S2, E2> Traversal.Admin<S2, E2> addStep(int index, Step<?, ?> step) throws IllegalStateException {
        this.steps.add(index, step);
        Step previousStep = this.steps.size() > 0 && index != 0 ? steps.get(index - 1) : null;
        Step nextStep = this.steps.size() > index + 1 ? steps.get(index + 1) : null;
        step.setPreviousStep(null != previousStep ? previousStep : EmptyStep.getInstance());
        step.setNextStep(null != nextStep ? nextStep : EmptyStep.getInstance());
        if (null != previousStep) previousStep.setNextStep(step);
        if (null != nextStep) nextStep.setPreviousStep(step);
        step.setTraversal(this);
        return (Traversal.Admin<S2, E2>)this;
    }

    @Override
    public <S2, E2> Traversal.Admin<S2, E2> replaceStep(int index, Step<?, ?> step) throws IllegalStateException {
        this.steps.remove(index);
        return addStep(index, step);
    }

    @Override
    public BasicGraph getGraph() {
        return this.graph;
    }

    @Override
    public void setGraph(BasicGraph graph) {
        this.graph = graph;
    }

    @Override
    public void applyStrategies() throws IllegalStateException {
        if (!isRoot()) {
            throw new IllegalStateException("only for root traversal");
        }
        if (this.locked) {
            throw new IllegalStateException("Traversal is locked");
        }

        TraversalHelper.applyTraversalRecursively(this::apply, this);

        this.locked = true;
    }

    protected void apply(final Traversal.Admin<?, ?> traversal) {
        // ================================
        // ================================
        // ================================
        // ================================
        // ================================
        // TODO
    }

    @Override
    public boolean isLocked() {
        return this.locked;
    }

    @Override
    public boolean isRoot() {
        return this.root;
    }

    @Override
    public void setRoot(boolean isRoot) {
        this.root = isRoot;
    }

    @Override
    public void addStart(Traverser.Admin<S> innerTraverser) {
        List<Traverser<S>> trList = new ArrayList<>();
        if (innerTraverser != null) {
            trList.add(innerTraverser);
        }

        if (getStartStep() instanceof StartStep) {
            ((StartStep<S>) getStartStep()).setTraverserList(trList);
        } else {
            StartStep startStep = new StartStep(this, trList);
            addStep(0, startStep);
        }
    }

    @Override
    public GraphTraversal.Admin<S, E> clone() {
        DefaultGraphTraversal clone = new DefaultGraphTraversal(this.graphTraversalSource, this.root);
        clone.locked = this.locked;
        clone.previousTraversal = this.previousTraversal;
        clone.sideEffectMap = this.sideEffectMap;
        clone.profile = new DefaultProfile();

        for (Step step : this.steps) {
            Step newStep = step.clone();
            newStep.setTraversal(this);
            clone.addStep(newStep);
        }

        return clone;
    }

    @Override
    public Traversal.Admin<S, E> getPreviousTraversal() {
        return this.previousTraversal;
    }

    @Override
    public void setPreviousTraversal(Traversal.Admin<S, E> traversal) {
        this.previousTraversal = traversal;
    }

    @Override
    public Object getSideEffect(String sideEffectKey) {
        return this.sideEffectMap.get(sideEffectKey);
    }

    @Override
    public void addSideEffect(String sideEffectKey, Object sideEffect) {
        this.sideEffectMap.put(sideEffectKey, sideEffect);
    }

    @Override
    public GraphTraversalSource getSource() {
        return graphTraversalSource;
    }

    @Override
    public void setSource(GraphTraversalSource source) {
        this.graphTraversalSource = source;
        this.graph = source != null ? source.getGraph() : null;
    }


}
