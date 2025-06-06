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

public class DefaultTraverser<T> implements Traverser.Admin<T> {

    private JsonObject source;
    private T obj;
    private Path path;

    protected DefaultTraverser() {
    }

    protected DefaultTraverser(T obj, Path path) {
        this.obj = obj;
        this.path = path;
    }

    public DefaultTraverser(T obj) {
        this.obj = obj;
        this.path = new DefaultPath().extend(this.obj);
    }

    public DefaultTraverser(JsonObject source, T obj) {
        this.source = source;
        this.obj = obj;
        this.path = new DefaultPath().extend(this.obj);
    }

    @Override
    public JsonObject getSource() {
        return this.source;
    }

    @Override
    public T get() {
        return this.obj;
    }

    @Override
    public Path path() {
        return this.path;
    }

    @Override
    public Traverser<T> clone() {
        DefaultTraverser<T> clone = new DefaultTraverser<T>();
        clone.source = source;
        clone.obj = obj;
        clone.path = this.path.clone();
        return clone;
    }

    @Override
    public <R> Admin<R> split(Step<T, R> step) {
        DefaultTraverser<R> result = new DefaultTraverser<>(
            (R)this.obj,
            this.path.clone().extend(this.obj, step.getLabels())
        );
        return result;
    }

    @Override
    public <R> Admin<R> split(R r, Step<T, R> step) {
        DefaultTraverser<R> result = new DefaultTraverser<>(
            r,
            this.path.clone().extend(r, step.getLabels().toArray(new String[]{}))
        );
        return result;
    }

    @Override
    public <R> Admin<R> split(JsonObject source, R r, Step<T, R> step) {
        DefaultTraverser<R> result = new DefaultTraverser<R>(source, r);
        result.path = this.path.clone().extend(r, step.getLabels().toArray(new String[]{}));
        return result;
    }

}
