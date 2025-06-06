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

import io.vertx.core.json.JsonObject;

public interface Traverser<T> extends Cloneable, Comparable<Traverser<T>> {

    default Admin<T> asAdmin() {
        return (Admin<T>) this;
    }

    JsonObject getSource();

    T get();

    Path path();

    @Override
    default int compareTo(final Traverser<T> other) throws ClassCastException {
        Object thisObj = (this.get() != null) ? this.get() : this.getSource();
        Object otherObj = (this.get() != null) ? other.get() : other.getSource();
        if (thisObj == otherObj) return 0;
        if (null == thisObj) return -1;
        if (null == otherObj) return 1;
        return ((Comparable) thisObj).compareTo(otherObj);
    }

    /*
     * Traverser cloning is important when splitting a traverser at a bifurcation point in a traversal.
     */
    @SuppressWarnings("CloneDoesntDeclareCloneNotSupportedException")
    Traverser<T> clone();

    interface Admin<T> extends Traverser<T> {

        <R> Admin<R> split(Step<T, R> step);

        <R> Admin<R> split(R r, Step<T, R> step);

        <R> Admin<R> split(JsonObject source, R r, Step<T, R> step);

    }
}
