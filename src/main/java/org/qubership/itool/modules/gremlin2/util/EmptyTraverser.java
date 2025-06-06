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

import org.qubership.itool.modules.gremlin2.Path;
import org.qubership.itool.modules.gremlin2.Step;
import org.qubership.itool.modules.gremlin2.Traverser;
import io.vertx.core.json.JsonObject;

public class EmptyTraverser<T> implements Traverser.Admin<T> {
    private static final EmptyTraverser INSTANCE = new EmptyTraverser<>();

    private EmptyTraverser() {
    }

    public static Traverser.Admin instance() {
        return INSTANCE;
    }

    @Override
    public JsonObject getSource() {
        return null;
    }

    @Override
    public T get() {
        return null;
    }

    @Override
    public Path path() {
        return null;
    }

    @Override
    public Traverser<T> clone() {
        return null;
    }

    @Override
    public <R> Admin<R> split(Step<T, R> step) {
        return (Admin<R>)this;
    }

    @Override
    public <R> Admin<R> split(R r, Step<T, R> step) {
        return (Admin<R>)this;
    }

    @Override
    public <R> Admin<R> split(JsonObject source, R r, Step<T, R> step) {
        return (Admin<R>)this;
    }

}
