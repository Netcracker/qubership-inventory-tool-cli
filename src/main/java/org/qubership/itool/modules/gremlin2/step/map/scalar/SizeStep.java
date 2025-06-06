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

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

@SuppressWarnings("rawtypes")
public class SizeStep<S> extends ScalarMapStep<S, Integer> {

    public SizeStep(Traversal.Admin traversal) {
        super(traversal);
    }

    @Override
    protected Integer map(Traverser.Admin<S> traverser) {
        int size = 0;
        S obj = traverser.get();

        if (obj instanceof List) {
            size = ((List)obj).size();
        } else if (obj instanceof JsonArray) {
            size = ((JsonArray)obj).size();
        } else if (obj instanceof Map) {
            size = ((Map)obj).size();
        } else if (obj instanceof JsonObject) {
            size = ((JsonObject)obj).size();
        } else if (obj instanceof Iterable) {
            Iterator iter = ((Iterable) obj).iterator();
            while (iter.hasNext()) {
                iter.next();
                size++;
            }
        } else if (obj instanceof String) {
            size = ((String)obj).length();
        }
        return size;
    }

    @Override
    protected Traverser<Integer> generateTraverser(Traverser.Admin<S> previousTraverser, Integer value) {
        Traverser<Integer> traverser = previousTraverser.split(
            (previousTraverser.getSource() != null) ? previousTraverser.getSource() : null,
            value, this);
        return traverser;
    }

}
