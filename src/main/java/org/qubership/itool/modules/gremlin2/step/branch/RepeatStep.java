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

package org.qubership.itool.modules.gremlin2.step.branch;

import org.qubership.itool.modules.gremlin2.Step;
import org.qubership.itool.modules.gremlin2.Traversal;
import org.qubership.itool.modules.gremlin2.Traverser;
import org.qubership.itool.modules.gremlin2.graph.lambda.TrueTraversal;
import org.qubership.itool.modules.gremlin2.step.AbstractStep;
import org.qubership.itool.modules.gremlin2.step.TimesModulating;
import org.qubership.itool.modules.gremlin2.util.TraversalHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class RepeatStep<S> extends AbstractStep implements TimesModulating {

    private Traversal.Admin<S, S> repeatTraversal = null;
    private Traversal.Admin<S, ?> untilTraversal = null;
    private Traversal.Admin<S, ?> emitTraversal = null;
    private String loopName = null;
    private boolean untilFirst = false;
    private boolean emitFirst = false;
    private int maxLoops = 0;

    @Override
    public String toString() {
        return getClass().getSimpleName() + "("
            + ((untilTraversal == null) ? "noUntil" : "until-" + (untilFirst ? "before" : "after"))
            + "," + ((emitTraversal == null) ? "noUntil" : "emit-" + (emitFirst ? "before" : "after"))
            + ")";
    }

    public RepeatStep(Traversal.Admin traversal) {
        super(traversal);
    }

    @Override
    public void clear() {
        clearTraversal(repeatTraversal);
        clearTraversal(untilTraversal);
        clearTraversal(emitTraversal);
    }

    @Override
    protected void processPreviousTraverser(Traverser.Admin previousTraverser, List result) {
        if (this.repeatTraversal == null) {
            throw new IllegalStateException("RepeatStep required repeat traversal");
        }

        List path = new ArrayList();
        List<Traverser<S>> tmpResultList = new ArrayList<>();
        tmpResultList.add(previousTraverser);
        int currentLoop = 0;

        do {
            tmpResultList = filterLoopedPath(path, tmpResultList);
            if (tmpResultList.size() == 0) {
                break;
            }

            currentLoop++;
            if (this.untilFirst && this.untilTraversal != null) {
                doUntil(result, tmpResultList);
            }
            if (this.emitFirst && this.emitTraversal != null) {
                doEmit(result, tmpResultList);
            }

            List<Traverser<S>> tmpList = new ArrayList<>();
            for (Traverser<S> traverser : tmpResultList) {
                Traversal.Admin<?, S> cloneTraversal_02 = prepareInnerTraversal(repeatTraversal, (Traverser.Admin)traverser);
                tmpList.addAll(cloneTraversal_02.getEndStep().getTraversers());
            }

            if (!this.untilFirst && this.untilTraversal != null) {
                doUntil(result, tmpList);
            }
            if (!this.emitFirst && this.emitTraversal != null) {
                doEmit(result, tmpList);
            }

            for (Traverser<S> traverser : tmpResultList) {
                path.add(traverser.get());
            }
            tmpResultList = tmpList;
        } while (loopHasNext(currentLoop, tmpResultList));

        if (this.emitTraversal == null || this.emitFirst) {
            result.addAll(tmpResultList);
        }
    }

    private List<Traverser<S>> filterLoopedPath(List path, List<Traverser<S>> tmpResultList) {
        if (path.size() > 10000) {
            throw new IllegalStateException("Too long path (>10k size)");
        }
        List<Traverser<S>> result = new ArrayList<>();
        int size = tmpResultList.size();
        for (int i=0 ; i<size ; i++) {
            Traverser<S> traverser = tmpResultList.get(i);
            if (!path.contains(traverser.get())) {
                result.add(traverser);
            }
        }
        return result;
    }

    private boolean loopHasNext(int currentLoop, List<Traverser<S>> tmpResultList) {
        if (this.maxLoops != 0) {
            return currentLoop < this.maxLoops;
        } else {
            return tmpResultList.size() != 0;
        }
        // TODO infinity loop
    }

    private void doUntil(List result, List<Traverser<S>> tmpList) {
        ListIterator<Traverser<S>> listIterator = tmpList.listIterator();
        while (listIterator.hasNext()) {
            Traverser.Admin<S> traverser = (Traverser.Admin<S>) listIterator.next();
            if (filterByTraversal(this.untilTraversal, traverser)) {
                result.add(traverser);
                listIterator.remove();
            }
        }
    }

    private void doEmit(List result, List<Traverser<S>> tmpResultList) {
        for (Traverser<S> traverser : tmpResultList) {
            if (filterByTraversal(this.emitTraversal, (Traverser.Admin)traverser)) {
                result.add(traverser);
            }
        }
    }

    private boolean filterByTraversal(Traversal.Admin<?, ?> traversal, Traverser.Admin<S> traverser) {
        if (traversal instanceof TrueTraversal) {
            return true;
        }

        Traversal.Admin<?, ?> cloneTraversal = prepareInnerTraversal(traversal, traverser);
        Object result = cloneTraversal.next();
        return (result != null);
    }

    @Override
    public void modulateTimes(int maxLoops) {
        this.maxLoops = maxLoops;
    }

    @Override
    public AbstractStep clone() {
        RepeatStep clone = (RepeatStep) super.clone();
        clone.repeatTraversal = repeatTraversal;
        clone.untilTraversal = untilTraversal;
        clone.emitTraversal = emitTraversal;
        clone.loopName = loopName;
        clone.emitFirst = emitFirst;
        clone.untilFirst = untilFirst;
        clone.maxLoops = maxLoops;
        return clone;
    }

    // Getter's and Setter's =============================================

    public Traversal.Admin<S, S> getRepeatTraversal() {
        return repeatTraversal;
    }

    public void setRepeatTraversal(Traversal.Admin<S, S> repeatTraversal) {
        this.repeatTraversal = repeatTraversal;
    }

    public Traversal.Admin<S, ?> getUntilTraversal() {
        return untilTraversal;
    }

    public void setUntilTraversal(Traversal.Admin<S, ?> untilTraversal) {
        this.untilTraversal = untilTraversal;
    }

    public Traversal.Admin<S, ?> getEmitTraversal() {
        return emitTraversal;
    }

    public void setEmitTraversal(Traversal.Admin<S, ?> emitTraversal) {
        this.emitTraversal = emitTraversal;
    }

    public boolean isUntilFirst() {
        return untilFirst;
    }

    public void setUntilFirst(boolean untilFirst) {
        this.untilFirst = untilFirst;
    }

    public boolean isEmitFirst() {
        return emitFirst;
    }

    public void setEmitFirst(boolean emitFirst) {
        this.emitFirst = emitFirst;
    }

    // Static method's ===================================================

    public static <A, B, C extends Traversal<A, B>> C addRepeatToTraversal(
        C traversal, Traversal.Admin<B, B> repeatTraversal) {
        Step<?, B> endStep = traversal.asAdmin().getEndStep();

        TraversalHelper.propagateSource(traversal, repeatTraversal);

        if ((endStep instanceof RepeatStep) && null == ((RepeatStep) endStep).repeatTraversal) {
            ((RepeatStep<B>) endStep).setRepeatTraversal(repeatTraversal);
        } else {
            RepeatStep<B> repeatStep = new RepeatStep<>(traversal.asAdmin());
            repeatStep.setRepeatTraversal(repeatTraversal);
            traversal.asAdmin().addStep(repeatStep);
        }
        return traversal;
    }

    public static <A, B, C extends Traversal<A, B>> C addUntilToTraversal(
        C traversal, Traversal.Admin<B, ?> untilPredicate) {
        Step<?, B> step = traversal.asAdmin().getEndStep();
        if (step instanceof RepeatStep && null == ((RepeatStep) step).untilTraversal) {
            ((RepeatStep<B>) step).setUntilTraversal(untilPredicate);
        } else {
            RepeatStep<B> repeatStep = new RepeatStep<>(traversal.asAdmin());
            repeatStep.setUntilTraversal(untilPredicate);
            repeatStep.setUntilFirst(true);
            traversal.asAdmin().addStep(repeatStep);
        }
        return traversal;
    }

    public static <A, B, C extends Traversal<A, B>> C addEmitToTraversal(
        C traversal, Traversal.Admin<B, ?> emitPredicate) {
        Step<?, B> step = traversal.asAdmin().getEndStep();
        if (step instanceof RepeatStep && null == ((RepeatStep) step).emitTraversal) {
            ((RepeatStep<B>) step).setEmitTraversal(emitPredicate);
        } else {
            RepeatStep<B> repeatStep = new RepeatStep<>(traversal.asAdmin());
            repeatStep.setEmitTraversal(emitPredicate);
            repeatStep.setEmitFirst(true);
            traversal.asAdmin().addStep(repeatStep);
        }
        return traversal;
    }

}
