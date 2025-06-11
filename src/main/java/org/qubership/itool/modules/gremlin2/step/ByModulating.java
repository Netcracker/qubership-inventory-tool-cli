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

package org.qubership.itool.modules.gremlin2.step;

import org.qubership.itool.modules.gremlin2.Traversal;
import org.qubership.itool.modules.gremlin2.structure.MapElement;
import org.qubership.itool.modules.gremlin2.util.Order;

public interface ByModulating {

    default void modulateBy(String string) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("not implemented yet");
    }

    default void modulateBy(String ... args) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("not implemented yet");
    }

    default void modulateBy(Traversal traversal) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("not implemented yet");
    }

    default void modulateBy(Order order) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("not implemented yet");
    }

    default void modulateBy(String propertyKey, Order order) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("not implemented yet");
    }

    default void modulateBy(MapElement mapElement) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("not implemented yet");
    }

}
