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
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PropertyValueStep<S, E> extends FlatMapStep<S, E> {

    public PropertyValueStep(Traversal.Admin traversal) {
        super(traversal);
    }

    @Override
    protected List<E> flatMap(Traverser.Admin<S> traverser) {
        List<E> result = new ArrayList<>();
        S obj = traverser.get();
        if (obj instanceof JsonObject) {
            JsonObject json = (JsonObject) obj;
            for (Object key : json.getMap().values()) {
                result.add((E)key);
            }
        } else if (obj instanceof Map) {
            Map map = (Map)obj;
            for (Object key : map.values()) {
                result.add((E)key);
            }
        } else {
            // nothing
        }
        return result;
    }

    @Override
    protected Traverser<E> generateTraverser(Traverser.Admin<S> previousTraverser, E value) {
        Traverser<E> traverser = previousTraverser.split(
            (previousTraverser.getSource() != null) ? previousTraverser.getSource() : null,
            value, this);
        return traverser;
    }

}
