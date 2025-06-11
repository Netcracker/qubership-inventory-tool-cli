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

import java.util.Collections;
import java.util.List;

public interface TraversalParent {

    default <S, E> List<Traversal.Admin<S, E>> getGlobalChildren() {
        return Collections.emptyList();
    }

    default <S, E> List<Traversal.Admin<S, E>> getLocalChildren() {
        return Collections.emptyList();
    }

    default void addLocalChild(final Traversal.Admin<?, ?> localChildTraversal) {
        throw new IllegalStateException("This traversal parent does not support the addition of local traversals: " + this.getClass().getCanonicalName());
    }

    default void addGlobalChild(final Traversal.Admin<?, ?> globalChildTraversal) {
        throw new IllegalStateException("This traversal parent does not support the addition of global traversals: " + this.getClass().getCanonicalName());
    }

    default void removeLocalChild(final Traversal.Admin<?, ?> localChildTraversal) {
        throw new IllegalStateException("This traversal parent does not support the removal of local traversals: " + this.getClass().getCanonicalName());
    }

    default void removeGlobalChild(final Traversal.Admin<?, ?> globalChildTraversal) {
        throw new IllegalStateException("This traversal parent does not support the removal of global traversals: " + this.getClass().getCanonicalName());
    }

}
