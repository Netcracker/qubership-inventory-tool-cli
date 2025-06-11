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

package org.qubership.itool.modules.gremlin2.step.map.scalar;

import org.qubership.itool.modules.gremlin2.Path;
import org.qubership.itool.modules.gremlin2.Traversal;
import org.qubership.itool.modules.gremlin2.Traverser;
import org.qubership.itool.modules.gremlin2.step.AbstractStep;
import org.qubership.itool.modules.gremlin2.step.ByModulating;
import org.qubership.itool.modules.gremlin2.step.FromToModulating;
import org.qubership.itool.modules.gremlin2.util.ValueHelper;

public class PathStep<S> extends ScalarMapStep<S, Path> implements FromToModulating, ByModulating {

    private String fromLabel;
    private String toLabel;
    private String modulateBy;

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(getClass().getSimpleName());
        if (modulateBy != null) {
            builder.append("(").append(modulateBy).append(")");
        }
        return builder.toString();
    }

    public PathStep(final Traversal.Admin traversal) {
        super(traversal);
    }

    @Override
    protected Path map(final Traverser.Admin<S> traverser) {
        Path path = traverser.path().clone(); // .subPath(this.fromLabel, this.toLabel);
        if (modulateBy != null) {
            for (int i=0 ;  i<path.objects().size() ; i++) {
                Object obj = path.objects().get(i);
                Object value = ValueHelper.getObjectValue(this.modulateBy, obj);
                path.objects().set(i, value); // TODO mutable structure. No way
            }
        }
        return path;
    }

    @Override
    public void modulateBy(String string) throws UnsupportedOperationException {
        this.modulateBy = string;
    }

    @Override
    public void addFrom(final String fromLabel) {
        this.fromLabel = fromLabel;
    }

    @Override
    public void addTo(final String toLabel) {
        this.toLabel = toLabel;
    }

    @Override
    public AbstractStep<S, Path> clone() {
        PathStep clone = (PathStep) super.clone();
        clone.fromLabel = this.fromLabel;
        clone.toLabel = this.toLabel;
        clone.modulateBy = this.modulateBy;
        return clone;
    }

}
