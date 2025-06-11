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

package org.qubership.itool.modules.gremlin2.step.filter;

import org.qubership.itool.modules.gremlin2.Traversal;
import org.qubership.itool.modules.gremlin2.Traverser;
import org.qubership.itool.modules.gremlin2.step.AbstractStep;
import org.qubership.itool.modules.gremlin2.step.ByModulating;
import org.qubership.itool.modules.gremlin2.util.ValueHelper;

import java.util.HashMap;
import java.util.Map;

public class DedupStep<S> extends FilterStep<S> implements ByModulating {

    private String modulateBy;
    private Map<S, S> origMap = new HashMap<>();

    @Override
    public String toString() {
        return getClass().getSimpleName() + (modulateBy == null ? "" : "(" + modulateBy + ")");
    }

    public DedupStep(Traversal.Admin traversal) {
        super(traversal);
    }

    @Override
    public void clear() {
        this.origMap = new HashMap<>();
    }

    @Override
    protected boolean filter(Traverser.Admin<S> traverser) {
        S obj = traverser.get();
        if (this.modulateBy != null) {
            obj = (S)ValueHelper.getObjectValue(this.modulateBy, obj);
            if (obj == null) {
                return false;
            }
        }
        if (this.origMap.containsKey(obj)) {
            return false;
        }
        this.origMap.put(obj, obj);
        return true;
    }

    @Override
    public void modulateBy(String string) throws UnsupportedOperationException {
        this.modulateBy = string;
    }

    @Override
    public AbstractStep<S, S> clone() {
        DedupStep clone = (DedupStep) super.clone();
        clone.modulateBy = this.modulateBy;
        clone.origMap = new HashMap();
        return clone;
    }

}
