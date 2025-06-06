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

import org.qubership.itool.modules.gremlin2.Traversal;
import org.qubership.itool.modules.gremlin2.Traverser;
import org.qubership.itool.modules.gremlin2.step.AbstractStep;
import org.qubership.itool.modules.gremlin2.step.ByModulating;
import org.qubership.itool.modules.gremlin2.util.ValueHelper;
import io.vertx.core.json.JsonObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class
ValueMapStep <K,E, E2> extends ScalarMapStep<E2, Map<K, E>> implements ByModulating {

    private String[] propertyKeys;

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + Arrays.asList(propertyKeys) + ")";
    }

    public <S, E> ValueMapStep(Traversal.Admin<S,E> traversal, String[] propertyKeys) {
        super(traversal);
        this.propertyKeys = propertyKeys;
    }

    @Override
    protected Map<K, E> map(Traverser.Admin<E2> traverser) {
        Map<K, E> map = new HashMap<>();
        E2 obj = traverser.get();
        if (obj instanceof JsonObject) {
            JsonObject json = (JsonObject) obj;
            if (propertyKeys.length == 0) {
                map = (Map<K, E>) json.getMap();

            } else {
                for (String key : this.propertyKeys) {
                    E tmp = (E) ValueHelper.getObjectValue(key, obj);
                    if (tmp != null) {
                        map.put((K) ValueHelper.getPropertyKey(key), tmp);
                    }
                }
            }

        } else if (obj instanceof Map) {
            Map tmpMap = (Map)obj;
            if (propertyKeys.length == 0) {
                map = (Map<K, E>) tmpMap;

            } else {
                for (String key : this.propertyKeys) {
//                    E tmp = (E) tmpMap.get(key);
                    E tmp = (E) ValueHelper.getObjectValue(key, tmpMap);
                    if (tmp != null) {
                        map.put((K) ValueHelper.getPropertyKey(key), tmp);
                    }
                }
            }
        }
        return map.size() == 0 ? null : map;
    }

    @Override
    protected Traverser<Map<K, E>> generateTraverser(Traverser.Admin<E2> previousTraverser, Map<K, E> value) {
        Traverser<Map<K, E>> traverser = previousTraverser.split(
            (previousTraverser.getSource() != null) ? previousTraverser.getSource() : null,
            value, this);
        return traverser;
    }

    @Override
    public AbstractStep<E2, Map<K, E>> clone() {
        ValueMapStep clone = (ValueMapStep) super.clone();
        clone.propertyKeys = this.propertyKeys;
        return clone;
    }
}
