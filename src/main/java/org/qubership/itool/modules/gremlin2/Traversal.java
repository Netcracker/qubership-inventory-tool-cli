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

package org.qubership.itool.modules.gremlin2;

import org.qubership.itool.modules.graph.BasicGraph;
import org.qubership.itool.modules.gremlin2.step.EmptyStep;
import org.qubership.itool.modules.gremlin2.step.util.Profile;

import java.util.*;

public interface Traversal<S, E> extends  Cloneable {

    default Traversal.Admin<S, E> asAdmin() {
        return (Traversal.Admin<S, E>) this;
    }

    @SuppressWarnings("CloneDoesntDeclareCloneNotSupportedException")
    Traversal<S, E> clone();

    default E next() {
        List<E> list = toList();
        return (list == null || list.size() == 0) ? null : list.get(0);
    }

    /**
     * Put all the results into an {@link ArrayList}.
     *
     * @return the results in a list
     */
    default List<E> toList() {
        return this.fill(new ArrayList<>());
    }

    /**
     * Put all the results into a {@link HashSet}.
     *
     * @return the results in a set
     */
    default Set<E> toSet() {
        return this.fill(new HashSet<>());
    }

    /**
     * Add all the results of the traversal to the provided collection.
     *
     * @param collection the collection to fill
     * @param <C> the collection type
     * @return the collection now filled
     */
    default <C extends Collection<E>> C fill(C collection) {
        Profile profile = (Profile) asAdmin().getSideEffect("__profile");
        if (profile != null) {
            profile.start();
        }

        if (!this.asAdmin().isLocked()) {
            this.asAdmin().applyStrategies();
        }
        Step<?, E> endStep = this.asAdmin().getEndStep();
        if (endStep == null) {
            return collection;
        }
        for (Traverser<E> traverser : endStep.getTraversers()) {
            collection.add(traverser.get());
        }

        if (profile != null) {
            profile.stop();
        }

        return collection;
    }

    interface Admin<S, E> extends Traversal<S, E> {
        Profile EMPTY_PROFILE = new Profile() {};

        default Profile getProfile() {
            return EMPTY_PROFILE;
        }

        List<Step> getSteps();

        default <E2> Traversal.Admin<S, E2> addStep(Step<?, E2> step) throws IllegalStateException {
            return this.addStep(this.getSteps().size(), step);
        }

        <S2, E2> Traversal.Admin<S2, E2> addStep(int index, Step<?, ?> step) throws IllegalStateException;

        <S2, E2> Traversal.Admin<S2, E2> replaceStep(int index, Step<?, ?> step) throws IllegalStateException;

        default Step<S, ?> getStartStep() {
            final List<Step> steps = this.getSteps();
            return steps.isEmpty() ? EmptyStep.getInstance() : steps.get(0);
        }

        default Step<?, E> getEndStep() {
            final List<Step> steps = this.getSteps();
            return steps.isEmpty() ? EmptyStep.getInstance() : steps.get(steps.size() - 1);
        }

        default Object getSideEffect(String sideEffectKey) {
            return null;
        }

        default  void addSideEffect(String sideEffectKey, Object sideEffect) {

        }

        @SuppressWarnings("CloneDoesntDeclareCloneNotSupportedException")
        Traversal.Admin<S, E> clone();

        Traversal.Admin<S, E> getPreviousTraversal();

        void setPreviousTraversal(Traversal.Admin<S, E> traversal);

        BasicGraph getGraph();
        void setGraph(BasicGraph graph);

        void applyStrategies() throws IllegalStateException;

        boolean isLocked();

        boolean isRoot();

        void setRoot(boolean isRoot);

        void addStart(Traverser.Admin<S> innerTraverser);

        default void clear() {
            for (Step step : getSteps()) {
                step.clear();
            }
        }

    }

}
