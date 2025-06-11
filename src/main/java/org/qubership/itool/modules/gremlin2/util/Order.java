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

package org.qubership.itool.modules.gremlin2.util;

import org.qubership.itool.modules.gremlin2.Traverser;
import io.vertx.core.json.JsonObject;

import java.util.Comparator;

public enum Order implements Comparator<Object> {

    asc {
        @Override
        public int compare(Object first, Object second) {
            if (first instanceof Traverser) {
                first = ((Traverser)first).get();
            }
            if (second instanceof Traverser) {
                second = ((Traverser)second).get();
            }

            if (Compare.bothAreNumber(first, second)) {
                if (first != null && second != null) {
                    return Compare.compareNumber((Number) first, (Number) second);
                } else if (first != null && second == null) {
                    return 1;
                } else if (first == null && second != null) {
                    return -1;
                } else {
                    return 0;
                }

            } else if (Compare.bothAreJsonObject(first, second)) {
                return Compare.compareJsonObject((JsonObject)first, (JsonObject)second);

            } else if (first instanceof Comparable) {
                Comparable firstComparable = (Comparable)first;
                return firstComparable.compareTo(second);
            }


            return 0;
        }

        @Override
        public Order reversed() {
            return desc;
        }
    },

    desc {
        @Override
        public int compare(Object first, Object second) {
            if (first instanceof Traverser) {
                first = ((Traverser)first).get();
            }
            if (second instanceof Traverser) {
                second = ((Traverser)second).get();
            }

            if (Compare.bothAreNumber(first, second)) {
                if (first != null && second != null) {
                    return Compare.compareNumber((Number) first, (Number) second) * (-1);
                } else if (first != null && second == null) {
                    return -1;
                } else if (first == null && second != null) {
                    return 1;
                } else {
                    return 0;
                }

            } else if (Compare.bothAreJsonObject(first, second)) {
                return Compare.compareJsonObject((JsonObject)first, (JsonObject)second) * (-1);

            } else if (first instanceof Comparable) {
                Comparable firstComparable = (Comparable)first;
                return firstComparable.compareTo(second) * (-1);
            }

            return 0;
        }

        @Override
        public Order reversed() {
            return asc;
        }
    }
}
