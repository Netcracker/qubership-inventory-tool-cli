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

package org.qubership.itool.modules.gremlin2.step.map.flat;

import org.qubership.itool.modules.gremlin2.Traversal;
import org.qubership.itool.modules.gremlin2.Traverser;
import org.qubership.itool.modules.gremlin2.step.AbstractStep;
import org.qubership.itool.modules.gremlin2.step.ByModulating;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class SplitStep<S, E> extends FlatMapStep<S, E> implements ByModulating {

    private String modulateByProperty;

    @Override
    public String toString() {
        return getClass().getSimpleName() + "("
            + (modulateByProperty == null ? "": modulateByProperty)
            + ")";
    }

    public SplitStep(Traversal.Admin traversal) {
        super(traversal);
    }

    @Override
    protected List<E> flatMap(Traverser.Admin<S> traverser) {
        List<E> result = new ArrayList<>();
        Object obj = traverser.get();
        if (!(obj instanceof String)) {
            result.add((E)obj);
        }

        String source = (String)obj;
        if (modulateByProperty != null) {
            for (String s : source.split(modulateByProperty)) {
                if (StringUtils.isNoneBlank(s)) {
                    result.add((E) s);
                }
            }

        } else {
            source.lines().forEach(s -> {
                if (StringUtils.isNoneBlank(s)) {
                    result.add((E) s);
                }
            });
        }

        return result;
    }

    @Override
    public void modulateBy(String string) throws UnsupportedOperationException {
        this.modulateByProperty = string;
    }

    @Override
    public AbstractStep<S, E> clone() {
        SplitStep clone = (SplitStep) super.clone();
        clone.modulateByProperty = this.modulateByProperty;
        return clone;
    }

}
