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

package org.qubership.itool.modules.gremlin2.step.map;

import org.qubership.itool.modules.gremlin2.Path;
import org.qubership.itool.modules.gremlin2.Traversal;
import org.qubership.itool.modules.gremlin2.Traverser;
import org.qubership.itool.modules.gremlin2.step.AbstractStep;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SelectStep<S, E> extends MapStep<S, Map<String, E>> {

    private String[] selectLabels;

    public SelectStep(Traversal.Admin traversal, String... selectLabels) {
        super(traversal);
        this.selectLabels = selectLabels;
    }

    @Override
    protected void processPreviousTraverser(Traverser.Admin<S> previousTraverser, List<Traverser<Map<String, E>>> result) {
        Map<String, E> map = new HashMap<>();
        Traverser<Map<String, E>> traverser = generateTraverser(previousTraverser, map);
        result.add(traverser);
        Path path = previousTraverser.path();
        for (String label : this.selectLabels) {
            E value = path.get(label);
            map.put(label, value);
        }
    }

    protected Traverser<Map<String, E>> generateTraverser(Traverser.Admin<S> previousTraverser, Map<String, E> value) {
        Traverser<Map<String, E>> traverser = previousTraverser.split(value, this);
        return traverser;
    }

    @Override
    public AbstractStep<S, Map<String, E>> clone() {
        SelectStep clone = (SelectStep) super.clone();
        clone.selectLabels = this.selectLabels;
        return clone;
    }

}
