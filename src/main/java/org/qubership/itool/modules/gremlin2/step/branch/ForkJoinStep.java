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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.qubership.itool.modules.gremlin2.Traversal;
import org.qubership.itool.modules.gremlin2.Traverser;
import org.qubership.itool.utils.FutureUtils;

public class ForkJoinStep<S, E> extends LocalStep<S, E> {

    protected static final Logger LOG = LoggerFactory.getLogger(ForkJoinStep.class);

    protected ForkJoinPool pool;

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public <E2> ForkJoinStep(Traversal.Admin traversal, Traversal<?, E2> innerTraversal, ForkJoinPool pool) {
        super(traversal, innerTraversal.asAdmin());
        this.pool = pool;
    }

    protected void processAllPreviousTraversers(List<Traverser<S>> previousTraversers, List<Traverser<E>> result) {
        List<RecursiveTask<List<Traverser<E>>>> subtasks = new ArrayList<>();
        for (Traverser<S> previousTraverser: previousTraversers) {
            @SuppressWarnings("serial")
            RecursiveTask<List<Traverser<E>>> subtask = new RecursiveTask<List<Traverser<E>>>() {
                @Override
                protected List<Traverser<E>> compute() {
                    List<Traverser<E>> subResults = new ArrayList<>();
                    // This call is thread-safe
                    processPreviousTraverser((Traverser.Admin<S>) previousTraverser, subResults);
                    return subResults;
                }
            };
            subtasks.add(subtask);
        }

        LOG.info("Forking to {} tasks", subtasks.size());
        List<List<Traverser<E>>> subResults = FutureUtils.invokeAndJoin(subtasks, pool);
        for (List<Traverser<E>> subResult: subResults) {
            result.addAll(subResult);
        }
    }

}
