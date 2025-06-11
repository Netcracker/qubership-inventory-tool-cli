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

import org.qubership.itool.modules.gremlin2.Traversal;
import org.qubership.itool.modules.gremlin2.Traverser;

import java.util.List;

public class SumStep<S> extends ReducingBarrierStep<S, Float> {

    public SumStep(Traversal.Admin traversal) {
        super(traversal);
    }

    @Override
    protected Float projectTraversers(List<Traverser<S>> previousTraversers) {
        float result = 0.0f;
        for (Traverser<S> traverser : previousTraversers) {
            S obj = traverser.get();
            if (obj instanceof Integer) {
                result += ((Integer)obj).floatValue();
            } else if (obj instanceof Float) {
                result += ((Float)obj).floatValue();
            } else if (obj instanceof Double) {
                result += ((Double)obj).floatValue();
            } else if (obj instanceof Long) {
                result += ((Long)obj).floatValue();
            }
        }
        return result;
    }

}
