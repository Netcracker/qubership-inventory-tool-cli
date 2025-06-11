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

package org.qubership.itool.modules.gremlin2.step.util;

import org.qubership.itool.modules.gremlin2.step.AbstractStep;

import java.util.List;
import java.util.function.Function;

public interface Profile {

    default <S, E> void profileStep(AbstractStep<S,E> step, Function<AbstractStep<S, E>, List> function) {
        long startTime = System.currentTimeMillis();
        List result = function.apply(step);
        long endTime = System.currentTimeMillis();

        if (!isStart()) {
            return;
        }
        addInfo(startTime, endTime, step, result);
    }

    default boolean isStart() {
        return false;
    }

    default void start() {
        // do nothing
    }

    default void stop() {
        // do nothing
    }

    default <S, E> void addInfo(long startTime, long endTime, AbstractStep<S,E> step, List result) {
        // do nothing
    }

}
