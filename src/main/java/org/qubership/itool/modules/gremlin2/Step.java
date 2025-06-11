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

package org.qubership.itool.modules.gremlin2;

import java.util.List;
import java.util.Set;

public interface Step<S, E> extends Cloneable {

    default void clear() {
        // do nothing by default
    }

    default boolean isIgnoreEmptyTraverser() {
        return false;
    }

    default void setIgnoreEmptyTraverser(boolean ignoreEmptyTraverser) {
    }

    void setPreviousStep(Step<?, S> step);

    Step<?, S> getPreviousStep();

    void setNextStep(Step<E, ?> step);

    Step<E, ?> getNextStep();

    void setTraversal(Traversal.Admin<?, ?> traversal);

    <A, B> Traversal.Admin<A, B> getTraversal();

    Set<String> getLabels();

    void addLabel(String label);

    void removeLabel(String label);

    @SuppressWarnings("CloneDoesntDeclareCloneNotSupportedException")
    Step<S, E> clone();

    List<Traverser<E>> getTraversers();

}
