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

import java.util.List;
import java.util.Optional;

public class RangeLimitStep<S> extends FilterStep<S> {

    private Optional<Integer> rangeFrom;
    private Optional<Integer> rangeTo;
    private int rangePosition = 0;
    private int rangeSize = 0;

    public RangeLimitStep(Traversal.Admin traversal, Optional<Integer> rangeFrom, Optional<Integer> rangeTo) {
        super(traversal);
        this.rangeFrom = rangeFrom;
        this.rangeTo = rangeTo;
    }

    @Override
    public void clear() {
        this.rangeSize = 0;
        this.rangePosition = 0;
    }

    @Override
    protected void processAllPreviousTraversers(List<Traverser<S>> previousTraversers, List<Traverser<S>> result) {
        this.rangeSize = previousTraversers.size();
        super.processAllPreviousTraversers(previousTraversers, result);
    }

    @Override
    protected boolean filter(Traverser.Admin<S> traverser) {
        boolean result = false;
        if (rangeFrom.isEmpty() && rangeTo.isEmpty()) {
            result = true;

        } else if (rangeFrom.isPresent() && rangeTo.isPresent()
            && this.rangePosition >= rangeFrom.get()
            && this.rangePosition < rangeTo.get()
        ) {
            result = true;

        } else if (rangeFrom.isPresent() && rangeTo.isEmpty()
            && this.rangePosition >= this.rangeSize - rangeFrom.get()
        ) {
            result = true;

        } else if (rangeFrom.isEmpty() && rangeTo.isPresent()
            && this.rangePosition < rangeTo.get()
        ) {
            result = true;
        }
        rangePosition += 1;
        return result;
    }

    @Override
    public AbstractStep<S, S> clone() {
        RangeLimitStep clone = (RangeLimitStep) super.clone();
        clone.rangeFrom = this.rangeFrom;
        clone.rangeTo = this.rangeTo;
        clone.rangePosition = 0;
        clone.rangeSize = 0;
        return clone;
    }

}
