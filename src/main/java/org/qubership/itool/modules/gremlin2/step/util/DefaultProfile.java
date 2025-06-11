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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultProfile implements Profile {

    public static final String TIME = "time";
    public static final String TRAVERSERS = "traversers";
    public static final String STEP = "step";
    public static final String DUR = "duration";

    private boolean started;
    private List<Map<String, Object>> steps = new ArrayList<>();

    @Override
    public <S, E> void addInfo(long startTime, long endTime, AbstractStep<S, E> step, List result) {
        Map<String, Object> entry = new HashMap<>();
        entry.put(TIME, (endTime - startTime));
        entry.put(TRAVERSERS, result.size());
        entry.put(STEP, step.toString());
        this.steps.add(entry);
    }

    @Override
    public boolean isStart() {
        return this.started;
    }

    @Override
    public void start() {
        this.started = true;
    }

    @Override
    public void stop() {
        this.started = false;
    }

    @Override
    public String toString() {
        long totalTime = 0;
        for (Map<String, Object> entry : this.steps) {
            totalTime+= (Long)entry.get(TIME);
        }
        for (Map<String, Object> entry : this.steps) {
            entry.put(DUR, (((Long)entry.get(TIME)).floatValue() / (float)totalTime * (float)100));
        }

        StringBuilder builder = new StringBuilder();
        builder.append("Profile{\n");
        // Step   Count   Traversers   Time (ms)   % Dur
        String header = String.format(
            "| %1$-60s | %2$-10s | %3$-10s | %4$-10s |"
            , "Step", "Traversers", "Time (ms)", "% Dur");
        builder.append("=".repeat(header.length())).append("\n");
        builder.append(header).append("\n");
        builder.append("=".repeat(header.length())).append("\n");
        for (Map<String, Object> entry : this.steps) {
            String line = String.format(
                "| %1$-60s | %2$10d | %3$10d | %4$10.2f |"
                , entry.get(STEP)
                , entry.get(TRAVERSERS)
                , entry.get(TIME)
                , entry.get(DUR)
            );
            builder.append(line).append("\n");
        }
        builder.append("=".repeat(header.length())).append("\n");
        String total = String.format(
            "| %1$60s | %2$10s | %3$10d | %4$10s |"
            , "Total:"
            , "-"
            , totalTime
            , "-"
        );
        builder.append(total).append("\n");
        builder.append("=".repeat(header.length())).append("\n");

        builder.append("}");
        return builder.toString();
    }

}
