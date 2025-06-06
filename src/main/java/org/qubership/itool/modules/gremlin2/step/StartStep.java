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

import org.qubership.itool.modules.gremlin2.Traversal;
import org.qubership.itool.modules.gremlin2.Traverser;

import java.util.ArrayList;
import java.util.List;

public class StartStep<S> extends AbstractStep<S, S> {

    protected List<Traverser<S>> traverserList;

    public StartStep(Traversal.Admin traversal) {
        super(traversal);
    }

    public StartStep(Traversal.Admin traversal, List<Traverser<S>> traverserList) {
        super(traversal);
        this.traverserList = traverserList;
    }

    @Override
    public void clear() {
        this.traverserList = new ArrayList<>();
    }

    public void setTraverserList(List<Traverser<S>> traverserList) {
        this.traverserList = traverserList;
    }

    @Override
    protected List<Traverser<S>> fetchPreviousTraversers() {
        List<Traverser<S>> result = new ArrayList<>();
        result.addAll(this.traverserList);
        return result;
    }

    @Override
    protected void processPreviousTraverser(Traverser.Admin<S> previousTraverser, List<Traverser<S>> result) {
        result.add(previousTraverser);
    }

}
