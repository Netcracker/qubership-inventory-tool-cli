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

import org.qubership.itool.modules.graph.BasicGraph;
import org.qubership.itool.modules.gremlin2.GremlinException;
import org.qubership.itool.modules.gremlin2.Step;
import org.qubership.itool.modules.gremlin2.Traversal;
import org.qubership.itool.modules.gremlin2.Traverser;
import org.qubership.itool.modules.gremlin2.util.EmptyTraversal;
import org.qubership.itool.modules.gremlin2.util.TraversalHelper;

import io.vertx.core.json.JsonObject;

import static org.qubership.itool.modules.graph.Graph.F_ID;

import java.util.*;

public abstract class AbstractStep<S, E> implements Step<S, E> {
    protected Set<String> labels = new HashSet<>();
    protected Traversal.Admin traversal;

    protected Step<?, S> previousStep = EmptyStep.getInstance();
    protected Step<E, ?> nextStep = EmptyStep.getInstance();

    protected boolean ignoreEmptyTraverser = false;

    public AbstractStep(Traversal.Admin traversal) {
        this.traversal = traversal;
    }

    public boolean isIgnoreEmptyTraverser() {
        return this.ignoreEmptyTraverser;
    }

    public void setIgnoreEmptyTraverser(boolean ignoreEmptyTraverser) {
        this.ignoreEmptyTraverser = ignoreEmptyTraverser;
    }

    protected Traversal.Admin fetchRootTraversal() {
        Traversal.Admin previousTraversal = this.traversal;
        while (previousTraversal.getPreviousTraversal() != null) {
            previousTraversal = previousTraversal.getPreviousTraversal();
        }
        return previousTraversal;
    }

    @Override
    public void addLabel(final String label) {
        this.labels.add(label);
    }

    @Override
    public void removeLabel(final String label) {
        this.labels.remove(label);
    }

    @Override
    public Set<String> getLabels() {
        return Collections.unmodifiableSet(this.labels);
    }

    @Override
    public void setPreviousStep(final Step<?, S> step) {
        this.previousStep = step;
    }

    @Override
    public Step<?, S> getPreviousStep() {
        return this.previousStep;
    }

    @Override
    public void setNextStep(final Step<E, ?> step) {
        this.nextStep = step;
    }

    @Override
    public Step<E, ?> getNextStep() {
        return this.nextStep;
    }

    @Override
    public <A, B> Traversal.Admin<A, B> getTraversal() {
        return this.traversal;
    }

    @Override
    public void setTraversal(final Traversal.Admin<?, ?> traversal) {
        this.traversal = traversal;
    }

    protected JsonObject requireSourceVertex(Traverser.Admin<JsonObject> traverser) throws GremlinException {
        JsonObject sourceVertex = null;
        BasicGraph graph = this.traversal.getGraph();
        if (graph != null) {
            sourceVertex = traverser.getSource();
            if (sourceVertex == null) {
                sourceVertex = traverser.get();
            }
        }
        if (sourceVertex == null || graph.getVertex(sourceVertex.getString(F_ID)) != sourceVertex) {
            throw new GremlinException("Step " + getClass().getSimpleName() + " applicable only to Vertex");
        }
        return sourceVertex;
    }

    @Override
    @SuppressWarnings("CloneDoesntDeclareCloneNotSupportedException")
    public AbstractStep<S, E> clone() {
        try {
            final AbstractStep<S, E> clone = (AbstractStep<S, E>) super.clone();
            clone.previousStep = EmptyStep.getInstance();
            clone.nextStep = EmptyStep.getInstance();
            clone.traversal = EmptyTraversal.getInstance();
            clone.labels = new HashSet<>(this.labels);
            clone.ignoreEmptyTraverser = this.ignoreEmptyTraverser;
            return clone;
        } catch (final CloneNotSupportedException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    @Override
    public List<Traverser<E>> getTraversers() {
        List<Traverser<E>> result = new ArrayList<>();
        List<Traverser<S>> previousTraversers = fetchPreviousTraversers();

        traversal.getProfile().profileStep(this, step -> {
            processAllPreviousTraversers(previousTraversers, result);
            filterEmptyTraverserIfRequired(result);
            applyLabelsModulator(result);
            return result;
        });

        return result;
    }

    protected List<Traverser<S>> fetchPreviousTraversers() {
        Step<?, S> previousStep = getPreviousStep();
        List<Traverser<S>> previousTraversers = previousStep.getTraversers();
        return previousTraversers;
    }

    protected void processAllPreviousTraversers(List<Traverser<S>> previousTraversers, List<Traverser<E>> result) {
        for (Traverser<S> previousTraverser : previousTraversers) {
            processPreviousTraverser((Traverser.Admin<S>) previousTraverser, result);
        }
    }

    protected void filterEmptyTraverserIfRequired(List<Traverser<E>> result) {
        if (isIgnoreEmptyTraverser()) {
            ListIterator<Traverser<E>> listIterator = result.listIterator();
            while (listIterator.hasNext()) {
                Traverser<E> traverser = listIterator.next();
                if (traverser.get() == null) {
                    listIterator.remove();
                }
            }
        }
    }

    protected void applyLabelsModulator(List<Traverser<E>> result) {
        if (this.labels.size() != 0) {
            for (Traverser<E> traverser : result) {
                traverser.path().extend(labels.toArray(new String[]{}));
            }
        }
    }

    protected abstract void processPreviousTraverser(Traverser.Admin<S> previousTraverser, List<Traverser<E>> result);

    protected Traversal.Admin<?, E> prepareInnerTraversal(Traversal.Admin<?, E> innerTraversal, Traverser.Admin<S> previousTraverser) {
        Traversal.Admin<?, E> cloneTraversal = innerTraversal.clone();
        cloneTraversal.clear();

        TraversalHelper.propagateSource(this.traversal, cloneTraversal);
        cloneTraversal.setPreviousTraversal(this.traversal);
        cloneTraversal.setRoot(true);
        cloneTraversal.setGraph(this.traversal.getGraph());

        cloneTraversal.addStart((Traverser.Admin)previousTraverser.clone());
        return cloneTraversal;
    }

    protected Traversal.Admin<?, E> prepareInnerTraversal(Traversal.Admin<?, E> innerTraversal, List<Traverser.Admin<S>> list) {
        Traversal.Admin<?, E> cloneTraversal = innerTraversal.clone();
        cloneTraversal.clear();

        TraversalHelper.propagateSource(this.traversal, cloneTraversal);
        cloneTraversal.setPreviousTraversal(this.traversal);
        cloneTraversal.setRoot(true);
        cloneTraversal.setGraph(this.traversal.getGraph());

        cloneTraversal.addStart(null);
        ((StartStep)cloneTraversal.getStartStep()).setTraverserList(list);
        return cloneTraversal;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    public void clearTraversal(Traversal.Admin traversal) {
        if (traversal != null) {
            traversal.clear();
        }
    }

}
