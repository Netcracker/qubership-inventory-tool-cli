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

import org.qubership.itool.modules.gremlin2.Step;
import org.qubership.itool.modules.gremlin2.Traversal;
import org.qubership.itool.modules.gremlin2.graph.DefaultGraphTraversal;
import org.qubership.itool.modules.gremlin2.graph.GraphTraversal;
import org.qubership.itool.modules.gremlin2.step.TraversalParent;
import org.qubership.itool.modules.gremlin2.step.filter.OrStep;
import org.qubership.itool.modules.gremlin2.step.filter.HasStep;
import org.qubership.itool.modules.gremlin2.step.util.HasContainer;
import org.qubership.itool.modules.gremlin2.step.util.HasContainerHolder;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

@SuppressWarnings({"rawtypes", "unchecked"})
public class TraversalHelper {

    private TraversalHelper() {
    }

    public static <S> void addToCollection(Collection<S> collection, S s) {
        if (s instanceof List) {
            collection.addAll((List)s);
        } else {
            collection.add(s);
        }
    }

    public static void applyTraversalRecursively(Consumer<Traversal.Admin<?, ?>> consumer, Traversal.Admin<?, ?> traversal) {
        consumer.accept(traversal);

        List<Step> steps = traversal.getSteps();
        for (int ix = 0; ix < steps.size(); ix++) {
            Step step = steps.get(ix);
            if (step instanceof TraversalParent) {
                for (final Traversal.Admin<?, ?> local : ((TraversalParent) step).getLocalChildren()) {
                    applyTraversalRecursively(consumer, local);
                }
                for (final Traversal.Admin<?, ?> global : ((TraversalParent) step).getGlobalChildren()) {
                    applyTraversalRecursively(consumer, global);
                }
            }
        }
    }

    public static <T extends Traversal.Admin<?, ?>> T addHasContainer(T traversal, HasContainer hasContainer) {
        if (traversal.getEndStep() instanceof HasContainerHolder) {
            ((HasContainerHolder) traversal.getEndStep()).addHasContainer(hasContainer);
            return traversal;
        }
        return (T) traversal.addStep(new HasStep<>(traversal, hasContainer));
    }

    public static <T extends Traversal.Admin<?, ?>> T addHasContainer(T traversal, String type, HasContainer hasContainer) {
        if (traversal.getEndStep() instanceof HasContainerHolder) {
            ((HasContainerHolder) traversal.getEndStep()).addHasContainer(hasContainer);
            return traversal;
        }
        return (T) traversal.addStep(new HasStep<>(traversal, type, hasContainer));
    }

    public static <T extends Traversal.Admin<?, ?>> T addHasOrContainer(T traversal, HasContainer hasContainer) {
        if (!(traversal.getEndStep() instanceof OrStep)) {
            traversal.addStep(new OrStep<>(traversal));
        }

        OrStep orStep = (OrStep)traversal.getEndStep();
        orStep.appendOrTraversals(new DefaultGraphTraversal<>().addStep(new HasStep<>(traversal, hasContainer)));

        return traversal;
    }

    public static void propagateSource(Traversal source, Traversal target) {
        if (source instanceof GraphTraversal && target instanceof GraphTraversal) {
            GraphTraversal.Admin targetAsAdmin = ((GraphTraversal)target).asAdmin();
            if (targetAsAdmin.getSource() == null) {
                targetAsAdmin.setSource(((GraphTraversal)source).asAdmin().getSource());
            }
        }

    }

}
