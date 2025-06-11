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

import org.qubership.itool.modules.gremlin2.P;
import org.qubership.itool.modules.gremlin2.util.Compare;

public class HasContainer {

    private String propertyKey;
    private P predicate;

    public HasContainer(String propertyKey, P<?> predicate) {
        this.propertyKey = propertyKey;
        this.predicate = predicate;
    }

    public boolean test(Object value) {
        return this.predicate.test(value);
    }

    public String getPropertyKey() {
        return propertyKey;
    }

    public P getPredicate() {
        return predicate;
    }

    @Override
    public String toString() {
        if (((Compare) predicate.getBiPredicate()) != null) {
            return propertyKey + "." + ((Compare) predicate.getBiPredicate()).name() + "(" + predicate.getValue() + ")";
        } else {
            return propertyKey + "." + predicate.toString();
        }
    }
}
