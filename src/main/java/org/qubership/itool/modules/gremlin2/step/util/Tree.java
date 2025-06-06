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

import java.util.HashMap;

public class Tree<T> extends HashMap<T, Tree<T>> {

    public Tree add(T obj) {
        if (!this.containsKey(obj)) {
            Tree<T> tree = new Tree<>();
            put(obj, tree);
            return tree;
        }
        return this.get(obj);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        int level = 0;
        builder.append("Tree{\n");
        generateString(builder, "", level, this);
        builder.append("}");
        return builder.toString();
    }

    private void generateString(StringBuilder builder, String prefix, int level, Tree<T> tree) {
        for (T key : tree.keySet()) {
            String innerPrefix = prefix + "| ";
            builder.append(prefix);
            builder.append(objectToString(key)).append("\n");
            Tree<T> child = tree.get(key);
            if (child != null && child.size() != 0) {
                generateString(builder, innerPrefix, level + 1, child);
            }
        }
    }

    private String objectToString(T key) {
//        if (key instanceof JsonObject) {
//            String id = ((JsonObject)key).getString("id");
//            if (id != null) {
//                return "[" + id + "]";
//            }
//        }
        return key.toString();
    }

}
