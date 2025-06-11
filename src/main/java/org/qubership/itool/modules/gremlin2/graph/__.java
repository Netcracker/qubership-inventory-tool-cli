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

package org.qubership.itool.modules.gremlin2.graph;

import org.qubership.itool.modules.gremlin2.P;
import org.qubership.itool.modules.gremlin2.Path;
import org.qubership.itool.modules.gremlin2.Traversal;
import org.qubership.itool.modules.gremlin2.step.util.Tree;
import org.qubership.itool.modules.gremlin2.util.Order;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.Map;

public class __ {

    protected __() {
    }

    public static <A> GraphTraversal<A, A> start() {
        return new DefaultGraphTraversal<>();
    }

    public static <A> GraphTraversal<A, A> none() {
        return __.<A>start().none();
    }

    public static <A> GraphTraversal<A, A> empty() {
        return __.<A>start().empty();
    }

    public static <A, B> GraphTraversal<A, B> local(Traversal<?, B> localTraversal) {
        return __.<A>start().local(localTraversal);
    }

    public static <A> GraphTraversal<A, A> mapToVertex() {
        return __.<A>start().mapToVertex();
    }

    public static <A> GraphTraversal<A, A> hasId(String ... ids) {
        return __.<A>start().hasId(ids);
    }

    public static <A> GraphTraversal<A, A> hasNotId(String ... ids) {
        return __.<A>start().hasNotId(ids);
    }

    public static <A> GraphTraversal<A, A> hasKey(String ... keys) {
        return __.<A>start().hasKey(keys);
    }

    public static <A> GraphTraversal<A, A> hasKeys(String ... keys) {
        return __.<A>start().hasKey(keys);
    }

    public static <A> GraphTraversal<A, A> hasNot(String ... keys) {
        return __.<A>start().hasNot(keys);
    }

    public static <A> GraphTraversal<A, A> hasType(String ... types) {
        return __.<A>start().hasType(types);
    }

    public static <A> GraphTraversal<A, A> hasTypes(String ... types) {
        return __.<A>start().hasType(types);
    }

    public static <A> GraphTraversal<A, A> has(String type, String propertyKey, String value) {
        return __.<A>start().has(type, propertyKey, value);
    }

    public static <A> GraphTraversal<A, A> has(String type, String propertyKey, P<?> predicate) {
        return __.<A>start().has(type, propertyKey, predicate);
    }

    public static <A> GraphTraversal<A, A> has(String propertyKey, String value) {
        return __.<A>start().has(propertyKey, value);
    }

    public static <A> GraphTraversal<A, A> has(String propertyKey, P<?> predicate) {
        return __.<A>start().has(propertyKey, predicate);
    }

    public static <A> GraphTraversal<A, String> id() {
        return __.<A>start().id();
    }

    public static <A> GraphTraversal<A, String> type() {
        return __.<A>start().type();
    }

    public static <A> GraphTraversal<A, String> name() {
        return __.<A>start().name();
    }

    public static <A> GraphTraversal<A, A> key() {
        return __.<A>start().key();
    }

    public static <A> GraphTraversal<A, A> value() {
        return __.<A>start().value();
    }

    public static <A> GraphTraversal<A, A> valueReplace(String regex, String replacement) {
        return __.<A>start().valueReplace(regex, replacement);
    }

    public static <A> GraphTraversal<A, Integer> size() {
        return __.<A>start().size();
    }

    public static <A> GraphTraversal<A, Tree> tree() {
        return __.<A>start().tree();
    }

    public static <A> GraphTraversal<A, A> as(String stepLabel, String... stepLabels) {
        return __.<A>start().as(stepLabel, stepLabels);
    }

    public static <A> GraphTraversal<A, A> select(String selectKey) {
        return __.<A>start().select(selectKey);
    }

    public static <A> GraphTraversal<A, A> cap(String sideEffectKey) {
        return __.<A>start().cap(sideEffectKey);
    }
    public static <A> GraphTraversal<A, Map<String, A>> select(String... selectKeys) {
        return __.<A>start().select(selectKeys);
    }

    public static <A> GraphTraversal<A, Path> path() {
        return __.<A>start().path();
    }

    public static <A> GraphTraversal<A, Long> count() {
        return __.<A>start().count();
    }

    public static <A> GraphTraversal<A, Float> sum() {
        return __.<A>start().sum();
    }

    public static <A> GraphTraversal<A, JsonObject> out(String... edgeLabels) {
        return __.<A>start().out(edgeLabels);
    }

    public static <A> GraphTraversal<A, JsonObject> in(String... edgeLabels) {
        return __.<A>start().in(edgeLabels);
    }

    public static <A> GraphTraversal<A, JsonObject> both(String... edgeLabels) {
        return __.<A>start().both(edgeLabels);
    }

    public static <A> GraphTraversal<A, JsonObject> outE(String... edgeLabels) {
        return __.<A>start().outE(edgeLabels);
    }

    public static <A> GraphTraversal<A, JsonObject> inE(String... edgeLabels) {
        return __.<A>start().inE(edgeLabels);
    }

    public static <A> GraphTraversal<A, JsonObject> bothE(String... edgeLabels) {
        return __.<A>start().bothE(edgeLabels);
    }

    public static <A> GraphTraversal<A, JsonObject> inV() {
        return __.<A>start().inV();
    }

    public static <A> GraphTraversal<A, JsonObject> outV() {
        return __.<A>start().outV();
    }

    public static <A> GraphTraversal<A, JsonObject> bothV() {
        return __.<A>start().bothV();
    }

    public static <A> GraphTraversal<A, A> value(String propertyKey) {
        return __.<A>start().value(propertyKey);
    }

    public static <A> GraphTraversal<A, Map<Object, A>> values(String... propertyKeys) {
        return __.<A>start().values(propertyKeys);
    }

    public static <A> GraphTraversal<A, Map<A, A>> group() {
        return __.<A>start().group();
    }

    public static <A> GraphTraversal<A, A> by(String string) {
        return __.<A>start().by(string);
    }

    public static <A> GraphTraversal<A, A> by(String ... args) {
        return __.<A>start().by(args);
    }

    public static <A> GraphTraversal<A, A> by(Traversal traversal) {
        return __.<A>start().by(traversal);
    }

    public static <A> GraphTraversal<A, A> by(Order order) {
        return __.<A>start().by(order);
    }

    public static <A> GraphTraversal<A, A> by(String propertyKey, Order order) {
        return __.<A>start().by(propertyKey, order);
    }

    public static <A> GraphTraversal<A, A> coalesce(Traversal<?, A>... coalesceTraversals) {
        return __.<A>start().coalesce(coalesceTraversals);
    }

    public static <A> GraphTraversal<A, A> is(Object value) {
        return __.<A>start().is(value);
    }

    public static <A> GraphTraversal<A, A> where(String startKey, P<String> predicate) {
        return __.<A>start().where(startKey, predicate);
    }

    public static <A> GraphTraversal<A, A> where(P<String> predicate) {
        return __.<A>start().where(predicate);
    }

    public static <A> GraphTraversal<A, A> where(Traversal<?, ?> innerTraversal) {
        return __.<A>start().where(innerTraversal);
    }

    public static <A> GraphTraversal<A, A> dedup() {
        return __.<A>start().dedup();
    }

    public static <A> GraphTraversal<A, A> union(Traversal<?, A> ... unionTraversals) {
        return __.<A>start().union(unionTraversals);
    }

    public static <A> GraphTraversal<A, A> not(Traversal<?, ?> innerTraversal) {
        return __.<A>start().not(innerTraversal);
    }

    public static <A> GraphTraversal<A, A> or(Traversal<?, A> ... orTraversal) {
        return __.<A>start().or(orTraversal);
    }

    public static <A> GraphTraversal<A, A> range(int rangeFrom, int rangeTo) {
        return __.<A>start().range(rangeFrom, rangeTo);
    }

    public static <A> GraphTraversal<A, A> limit(int limit) {
        return __.<A>start().limit(limit);
    }

    public static <A> GraphTraversal<A, A> tail(int rangeFrom) {
        return __.<A>start().tail(rangeFrom);
    }

    public static <A> GraphTraversal<A, A> order() {
        return __.<A>start().order();
    }

    public static <A extends String> GraphTraversal<String, String> split() {
        return __.<A>start().split();
    }

    public static <A> GraphTraversal<A, A> unfold() {
        return __.<A>start().unfold();
    }

    public static <A> GraphTraversal<A, List<A>> fold() {
        return __.<A>start().fold();
    }

    public static <A> GraphTraversal<A, JsonObject> subgraph(String sideEffectKey) {
        return __.<A>start().subgraph(sideEffectKey);
    }

    public static <A> GraphTraversal<A, A> repeat(Traversal<?, A> repeatTraversal) {
        return __.<A>start().repeat(repeatTraversal);
    }

    public static <A> GraphTraversal<A, A> times(int maxLoops) {
        return __.<A>start().times(maxLoops);
    }

    public static <A> GraphTraversal<A, A> until(final Traversal<?, ?> untilTraversal) {
        return __.<A>start().until(untilTraversal);
    }

    public static <A> GraphTraversal<A, A> emit() {
        return __.<A>start().emit();
    }

    public static <A> GraphTraversal<A, A> emit(Traversal<?, ?> emitTraversal) {
        return __.<A>start().emit(emitTraversal);
    }

    public static <A> GraphTraversal<A, JsonObject> glob(String pattern) {
        return __.<A>start().glob(pattern);
    }

}
