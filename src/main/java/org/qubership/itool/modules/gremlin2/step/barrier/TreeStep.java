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

package org.qubership.itool.modules.gremlin2.step.barrier;

import org.qubership.itool.modules.gremlin2.Path;
import org.qubership.itool.modules.gremlin2.Traversal;
import org.qubership.itool.modules.gremlin2.Traverser;
import org.qubership.itool.modules.gremlin2.step.ByModulating;
import org.qubership.itool.modules.gremlin2.step.util.Tree;
import org.qubership.itool.modules.gremlin2.util.ValueHelper;

import java.util.List;

public class TreeStep<S> extends ReducingBarrierStep<S, Tree> implements ByModulating {

    private String modulateBy;

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + modulateBy + ")";
    }

    public TreeStep(Traversal.Admin traversal) {
        super(traversal);
    }

    @Override
    protected Tree projectTraversers(List<Traverser<S>> previousTraversers) {
        Tree tree = new Tree();
        for (Traverser<S> traverser : previousTraversers) {
            Path path = traverser.path();
            List pathObjectList = path.objects();
            Tree innerTree = tree;
            for (Object pathObj : pathObjectList) {
                Object value = pathObj;
                if (this.modulateBy != null) {
                    value = ValueHelper.getObjectValue(this.modulateBy, pathObj);
                }
                innerTree = innerTree.add(value);
            }
        }
        return tree;
    }

    @Override
    public void modulateBy(String string) throws UnsupportedOperationException {
        this.modulateBy = string;
    }

    @Override
    public TreeStep<S> clone() {
        TreeStep<S> clone = (TreeStep<S>) super.clone();
        clone.modulateBy = modulateBy;
        return clone;
    }

}
