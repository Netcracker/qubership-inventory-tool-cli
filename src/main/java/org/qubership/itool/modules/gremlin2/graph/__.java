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

/**
 * Utility class providing static methods for graph traversal operations.
 * This class serves as a facade for common graph traversal patterns used
 * throughout the application.
 */
public class __ {

    /**
     * Protected constructor to prevent instantiation of this utility class.
     */
    protected __() {
    }

    /**
     * Creates a new graph traversal starting point.
     *
     * @param <A> the type of the traversal
     * @return a new graph traversal
     */
    public static <A> GraphTraversal<A, A> start() {
        return new DefaultGraphTraversal<>();
    }

    /**
     * Creates a traversal that yields no results.
     *
     * @param <A> the type of the traversal
     * @return a traversal that yields no results
     */
    public static <A> GraphTraversal<A, A> none() {
        return __.<A>start().none();
    }

    /**
     * Creates an empty traversal.
     *
     * @param <A> the type of the traversal
     * @return an empty traversal
     */
    public static <A> GraphTraversal<A, A> empty() {
        return __.<A>start().empty();
    }

    /**
     * Applies a local traversal to each element.
     *
     * @param <A> the input type
     * @param <B> the output type
     * @param localTraversal the local traversal to apply
     * @return a traversal with the local transformation applied
     */
    public static <A, B> GraphTraversal<A, B> local(Traversal<?, B> localTraversal) {
        return __.<A>start().local(localTraversal);
    }

    /**
     * Maps the traversal to vertex objects.
     *
     * @param <A> the type of the traversal
     * @return a traversal that yields vertex objects
     */
    public static <A> GraphTraversal<A, A> mapToVertex() {
        return __.<A>start().mapToVertex();
    }

    /**
     * Filters vertices by their IDs.
     *
     * @param <A> the type of the traversal
     * @param ids the IDs to filter by
     * @return a traversal filtered by vertex IDs
     */
    public static <A> GraphTraversal<A, A> hasId(String ... ids) {
        return __.<A>start().hasId(ids);
    }

    /**
     * Filters vertices that do not have the specified IDs.
     *
     * @param <A> the type of the traversal
     * @param ids the IDs to exclude
     * @return a traversal excluding vertices with the specified IDs
     */
    public static <A> GraphTraversal<A, A> hasNotId(String ... ids) {
        return __.<A>start().hasNotId(ids);
    }

    /**
     * Filters vertices that have the specified keys.
     *
     * @param <A> the type of the traversal
     * @param keys the keys to filter by
     * @return a traversal filtered by vertex keys
     */
    public static <A> GraphTraversal<A, A> hasKey(String ... keys) {
        return __.<A>start().hasKey(keys);
    }

    /**
     * Filters vertices that have all of the specified keys.
     *
     * @param <A> the type of the traversal
     * @param keys the keys that must all be present
     * @return a traversal filtered by vertex keys
     */
    public static <A> GraphTraversal<A, A> hasKeys(String ... keys) {
        return __.<A>start().hasKey(keys);
    }

    /**
     * Filters vertices that do not have the specified keys.
     *
     * @param <A> the type of the traversal
     * @param keys the keys to exclude
     * @return a traversal excluding vertices with the specified keys
     */
    public static <A> GraphTraversal<A, A> hasNot(String ... keys) {
        return __.<A>start().hasNot(keys);
    }

    /**
     * Filters vertices by their type.
     *
     * @param <A> the type of the traversal
     * @param types the types to filter by
     * @return a traversal filtered by vertex type
     */
    public static <A> GraphTraversal<A, A> hasType(String ... types) {
        return __.<A>start().hasType(types);
    }

    /**
     * Filters vertices that have all of the specified types.
     *
     * @param <A> the type of the traversal
     * @param types the types that must all be present
     * @return a traversal filtered by vertex types
     */
    public static <A> GraphTraversal<A, A> hasTypes(String ... types) {
        return __.<A>start().hasType(types);
    }

    /**
     * Filters vertices by type, property key, and value.
     *
     * @param <A> the type of the traversal
     * @param type the vertex type
     * @param propertyKey the property key
     * @param value the property value
     * @return a traversal filtered by type, property key, and value
     */
    public static <A> GraphTraversal<A, A> has(String type, String propertyKey, String value) {
        return __.<A>start().has(type, propertyKey, value);
    }

    /**
     * Filters vertices by type, property key, and predicate.
     *
     * @param <A> the type of the traversal
     * @param type the vertex type
     * @param propertyKey the property key
     * @param predicate the predicate to apply
     * @return a traversal filtered by type, property key, and predicate
     */
    public static <A> GraphTraversal<A, A> has(String type, String propertyKey, P<?> predicate) {
        return __.<A>start().has(type, propertyKey, predicate);
    }

    /**
     * Filters vertices by property key and value.
     *
     * @param <A> the type of the traversal
     * @param propertyKey the property key
     * @param value the property value
     * @return a traversal filtered by property key and value
     */
    public static <A> GraphTraversal<A, A> has(String propertyKey, String value) {
        return __.<A>start().has(propertyKey, value);
    }

    /**
     * Filters vertices by property key and predicate.
     *
     * @param <A> the type of the traversal
     * @param propertyKey the property key
     * @param predicate the predicate to apply
     * @return a traversal filtered by property key and predicate
     */
    public static <A> GraphTraversal<A, A> has(String propertyKey, P<?> predicate) {
        return __.<A>start().has(propertyKey, predicate);
    }

    /**
     * Projects the traversal to vertex IDs.
     *
     * @param <A> the type of the traversal
     * @return a traversal that yields vertex IDs
     */
    public static <A> GraphTraversal<A, String> id() {
        return __.<A>start().id();
    }

    /**
     * Projects the traversal to vertex types.
     *
     * @param <A> the type of the traversal
     * @return a traversal that yields vertex types
     */
    public static <A> GraphTraversal<A, String> type() {
        return __.<A>start().type();
    }

    /**
     * Projects the traversal to vertex names.
     *
     * @param <A> the type of the traversal
     * @return a traversal that yields vertex names
     */
    public static <A> GraphTraversal<A, String> name() {
        return __.<A>start().name();
    }

    /**
     * Projects the traversal to property keys.
     *
     * @param <A> the type of the traversal
     * @return a traversal that yields property keys
     */
    public static <A> GraphTraversal<A, A> key() {
        return __.<A>start().key();
    }

    /**
     * Projects the traversal to property values.
     *
     * @param <A> the type of the traversal
     * @return a traversal that yields property values
     */
    public static <A> GraphTraversal<A, A> value() {
        return __.<A>start().value();
    }

    /**
     * Replaces values in the traversal using regex.
     *
     * @param <A> the type of the traversal
     * @param regex the regex pattern
     * @param replacement the replacement string
     * @return a traversal with replaced values
     */
    public static <A> GraphTraversal<A, A> valueReplace(String regex, String replacement) {
        return __.<A>start().valueReplace(regex, replacement);
    }

    /**
     * Counts the number of elements in the traversal.
     *
     * @param <A> the type of the traversal
     * @return a traversal that yields the count
     */
    public static <A> GraphTraversal<A, Integer> size() {
        return __.<A>start().size();
    }

    /**
     * Creates a tree structure from the traversal.
     *
     * @param <A> the type of the traversal
     * @return a traversal that yields a tree structure
     */
    public static <A> GraphTraversal<A, Tree> tree() {
        return __.<A>start().tree();
    }

    /**
     * Labels the current step for later reference.
     *
     * @param <A> the type of the traversal
     * @param stepLabel the label for the current step
     * @param stepLabels additional labels
     * @return a labeled traversal
     */
    public static <A> GraphTraversal<A, A> as(String stepLabel, String... stepLabels) {
        return __.<A>start().as(stepLabel, stepLabels);
    }

    /**
     * Selects a previously labeled step.
     *
     * @param <A> the type of the traversal
     * @param selectKey the key of the step to select
     * @return a traversal with the selected step
     */
    public static <A> GraphTraversal<A, A> select(String selectKey) {
        return __.<A>start().select(selectKey);
    }

    /**
     * Captures a side effect with the given key.
     *
     * @param <A> the type of the traversal
     * @param sideEffectKey the key for the side effect
     * @return a traversal with captured side effects
     */
    public static <A> GraphTraversal<A, A> cap(String sideEffectKey) {
        return __.<A>start().cap(sideEffectKey);
    }

    /**
     * Selects multiple previously labeled steps.
     *
     * @param <A> the type of the traversal
     * @param selectKeys the keys of the steps to select
     * @return a traversal with the selected steps
     */
    public static <A> GraphTraversal<A, Map<String, A>> select(String... selectKeys) {
        return __.<A>start().select(selectKeys);
    }

    /**
     * Projects the traversal to paths.
     *
     * @param <A> the type of the traversal
     * @return a traversal that yields paths
     */
    public static <A> GraphTraversal<A, Path> path() {
        return __.<A>start().path();
    }

    /**
     * Counts the number of elements in the traversal.
     *
     * @param <A> the type of the traversal
     * @return a traversal that yields the count as a long
     */
    public static <A> GraphTraversal<A, Long> count() {
        return __.<A>start().count();
    }

    /**
     * Sums the numeric values in the traversal.
     *
     * @param <A> the type of the traversal
     * @return a traversal that yields the sum
     */
    public static <A> GraphTraversal<A, Float> sum() {
        return __.<A>start().sum();
    }

    /**
     * Traverses to outgoing vertices.
     *
     * @param <A> the type of the traversal
     * @param edgeLabels the edge labels to traverse
     * @return a traversal to outgoing vertices
     */
    public static <A> GraphTraversal<A, JsonObject> out(String... edgeLabels) {
        return __.<A>start().out(edgeLabels);
    }

    /**
     * Traverses to incoming vertices.
     *
     * @param <A> the type of the traversal
     * @param edgeLabels the edge labels to traverse
     * @return a traversal to incoming vertices
     */
    public static <A> GraphTraversal<A, JsonObject> in(String... edgeLabels) {
        return __.<A>start().in(edgeLabels);
    }

    /**
     * Traverses to both incoming and outgoing vertices.
     *
     * @param <A> the type of the traversal
     * @param edgeLabels the edge labels to traverse
     * @return a traversal to both incoming and outgoing vertices
     */
    public static <A> GraphTraversal<A, JsonObject> both(String... edgeLabels) {
        return __.<A>start().both(edgeLabels);
    }

    /**
     * Traverses to outgoing edges.
     *
     * @param <A> the type of the traversal
     * @param edgeLabels the edge labels to traverse
     * @return a traversal to outgoing edges
     */
    public static <A> GraphTraversal<A, JsonObject> outE(String... edgeLabels) {
        return __.<A>start().outE(edgeLabels);
    }

    /**
     * Traverses to incoming edges.
     *
     * @param <A> the type of the traversal
     * @param edgeLabels the edge labels to traverse
     * @return a traversal to incoming edges
     */
    public static <A> GraphTraversal<A, JsonObject> inE(String... edgeLabels) {
        return __.<A>start().inE(edgeLabels);
    }

    /**
     * Traverses to both incoming and outgoing edges.
     *
     * @param <A> the type of the traversal
     * @param edgeLabels the edge labels to traverse
     * @return a traversal to both incoming and outgoing edges
     */
    public static <A> GraphTraversal<A, JsonObject> bothE(String... edgeLabels) {
        return __.<A>start().bothE(edgeLabels);
    }

    /**
     * Traverses to incoming vertices of edges.
     *
     * @param <A> the type of the traversal
     * @return a traversal to incoming vertices of edges
     */
    public static <A> GraphTraversal<A, JsonObject> inV() {
        return __.<A>start().inV();
    }

    /**
     * Traverses to outgoing vertices of edges.
     *
     * @param <A> the type of the traversal
     * @return a traversal to outgoing vertices of edges
     */
    public static <A> GraphTraversal<A, JsonObject> outV() {
        return __.<A>start().outV();
    }

    /**
     * Traverses to both incoming and outgoing vertices of edges.
     *
     * @param <A> the type of the traversal
     * @return a traversal to both incoming and outgoing vertices of edges
     */
    public static <A> GraphTraversal<A, JsonObject> bothV() {
        return __.<A>start().bothV();
    }

    /**
     * Projects the traversal to a specific property value.
     *
     * @param <A> the type of the traversal
     * @param propertyKey the property key
     * @return a traversal that yields the property value
     */
    public static <A> GraphTraversal<A, A> value(String propertyKey) {
        return __.<A>start().value(propertyKey);
    }

    /**
     * Projects the traversal to multiple property values.
     *
     * @param <A> the type of the traversal
     * @param propertyKeys the property keys
     * @return a traversal that yields property values
     */
    public static <A> GraphTraversal<A, Map<Object, A>> values(String... propertyKeys) {
        return __.<A>start().values(propertyKeys);
    }

    /**
     * Groups the traversal results.
     *
     * @param <A> the type of the traversal
     * @return a traversal that yields grouped results
     */
    public static <A> GraphTraversal<A, Map<A, A>> group() {
        return __.<A>start().group();
    }

    /**
     * Specifies the grouping key.
     *
     * @param <A> the type of the traversal
     * @param string the grouping key
     * @return a traversal with grouping specified
     */
    public static <A> GraphTraversal<A, A> by(String string) {
        return __.<A>start().by(string);
    }

    /**
     * Specifies multiple grouping keys.
     *
     * @param <A> the type of the traversal
     * @param args the grouping keys
     * @return a traversal with multiple grouping keys specified
     */
    public static <A> GraphTraversal<A, A> by(String ... args) {
        return __.<A>start().by(args);
    }

    /**
     * Specifies grouping by a traversal.
     *
     * @param <A> the type of the traversal
     * @param traversal the traversal to use for grouping
     * @return a traversal with traversal-based grouping specified
     */
    public static <A> GraphTraversal<A, A> by(Traversal traversal) {
        return __.<A>start().by(traversal);
    }

    /**
     * Specifies grouping by order.
     *
     * @param <A> the type of the traversal
     * @param order the order to use for grouping
     * @return a traversal with order-based grouping specified
     */
    public static <A> GraphTraversal<A, A> by(Order order) {
        return __.<A>start().by(order);
    }

    /**
     * Specifies grouping by property key and order.
     *
     * @param <A> the type of the traversal
     * @param propertyKey the property key
     * @param order the order to use for grouping
     * @return a traversal with property key and order-based grouping specified
     */
    public static <A> GraphTraversal<A, A> by(String propertyKey, Order order) {
        return __.<A>start().by(propertyKey, order);
    }

    /**
     * Returns the first non-null result from multiple traversals.
     *
     * @param <A> the type of the traversal
     * @param coalesceTraversals the traversals to try in order
     * @return a traversal that returns the first non-null result
     */
    public static <A> GraphTraversal<A, A> coalesce(Traversal<?, A>... coalesceTraversals) {
        return __.<A>start().coalesce(coalesceTraversals);
    }

    /**
     * Filters elements that equal the specified value.
     *
     * @param <A> the type of the traversal
     * @param value the value to compare against
     * @return a traversal filtered by equality
     */
    public static <A> GraphTraversal<A, A> is(Object value) {
        return __.<A>start().is(value);
    }

    /**
     * Filters elements based on a predicate applied to a specific key.
     *
     * @param <A> the type of the traversal
     * @param startKey the key to apply the predicate to
     * @param predicate the predicate to apply
     * @return a traversal filtered by the predicate
     */
    public static <A> GraphTraversal<A, A> where(String startKey, P<String> predicate) {
        return __.<A>start().where(startKey, predicate);
    }

    /**
     * Filters elements based on a predicate.
     *
     * @param <A> the type of the traversal
     * @param predicate the predicate to apply
     * @return a traversal filtered by the predicate
     */
    public static <A> GraphTraversal<A, A> where(P<String> predicate) {
        return __.<A>start().where(predicate);
    }

    /**
     * Filters elements based on an inner traversal.
     *
     * @param <A> the type of the traversal
     * @param innerTraversal the inner traversal to use for filtering
     * @return a traversal filtered by the inner traversal
     */
    public static <A> GraphTraversal<A, A> where(Traversal<?, ?> innerTraversal) {
        return __.<A>start().where(innerTraversal);
    }

    /**
     * Removes duplicate elements from the traversal.
     *
     * @param <A> the type of the traversal
     * @return a traversal with duplicates removed
     */
    public static <A> GraphTraversal<A, A> dedup() {
        return __.<A>start().dedup();
    }

    /**
     * Combines multiple traversals using union.
     *
     * @param <A> the type of the traversal
     * @param unionTraversals the traversals to union
     * @return a traversal that combines all results
     */
    public static <A> GraphTraversal<A, A> union(Traversal<?, A> ... unionTraversals) {
        return __.<A>start().union(unionTraversals);
    }

    /**
     * Filters elements that do not match the inner traversal.
     *
     * @param <A> the type of the traversal
     * @param innerTraversal the inner traversal to negate
     * @return a traversal with elements that don't match the inner traversal
     */
    public static <A> GraphTraversal<A, A> not(Traversal<?, ?> innerTraversal) {
        return __.<A>start().not(innerTraversal);
    }

    /**
     * Combines multiple traversals using logical OR.
     *
     * @param <A> the type of the traversal
     * @param orTraversal the traversals to combine with OR
     * @return a traversal that matches any of the conditions
     */
    public static <A> GraphTraversal<A, A> or(Traversal<?, A> ... orTraversal) {
        return __.<A>start().or(orTraversal);
    }

    /**
     * Limits the traversal to a specific range.
     *
     * @param <A> the type of the traversal
     * @param rangeFrom the starting index (inclusive)
     * @param rangeTo the ending index (exclusive)
     * @return a traversal limited to the specified range
     */
    public static <A> GraphTraversal<A, A> range(int rangeFrom, int rangeTo) {
        return __.<A>start().range(rangeFrom, rangeTo);
    }

    /**
     * Limits the traversal to a maximum number of results.
     *
     * @param <A> the type of the traversal
     * @param limit the maximum number of results
     * @return a traversal limited to the specified number of results
     */
    public static <A> GraphTraversal<A, A> limit(int limit) {
        return __.<A>start().limit(limit);
    }

    /**
     * Limits the traversal to the last N results.
     *
     * @param <A> the type of the traversal
     * @param rangeFrom the number of results to take from the end
     * @return a traversal limited to the last N results
     */
    public static <A> GraphTraversal<A, A> tail(int rangeFrom) {
        return __.<A>start().tail(rangeFrom);
    }

    /**
     * Orders the traversal results.
     *
     * @param <A> the type of the traversal
     * @return a traversal with ordered results
     */
    public static <A> GraphTraversal<A, A> order() {
        return __.<A>start().order();
    }

    /**
     * Splits string values in the traversal.
     *
     * @param <A> the type of the traversal (must extend String)
     * @return a traversal that yields split strings
     */
    public static <A extends String> GraphTraversal<String, String> split() {
        return __.<A>start().split();
    }

    /**
     * Unfolds collections in the traversal.
     *
     * @param <A> the type of the traversal
     * @return a traversal with unfolded collections
     */
    public static <A> GraphTraversal<A, A> unfold() {
        return __.<A>start().unfold();
    }

    /**
     * Folds the traversal into a list.
     *
     * @param <A> the type of the traversal
     * @return a traversal that yields a list of all elements
     */
    public static <A> GraphTraversal<A, List<A>> fold() {
        return __.<A>start().fold();
    }

    /**
     * Creates a subgraph from the traversal.
     *
     * @param <A> the type of the traversal
     * @param sideEffectKey the key to store the subgraph under
     * @return a traversal that creates a subgraph
     */
    public static <A> GraphTraversal<A, JsonObject> subgraph(String sideEffectKey) {
        return __.<A>start().subgraph(sideEffectKey);
    }

    /**
     * Repeats a traversal until a condition is met.
     *
     * @param <A> the type of the traversal
     * @param repeatTraversal the traversal to repeat
     * @return a traversal that repeats the specified traversal
     */
    public static <A> GraphTraversal<A, A> repeat(Traversal<?, A> repeatTraversal) {
        return __.<A>start().repeat(repeatTraversal);
    }

    /**
     * Specifies the number of times to repeat a traversal.
     *
     * @param <A> the type of the traversal
     * @param maxLoops the maximum number of loops
     * @return a traversal with the specified number of loops
     */
    public static <A> GraphTraversal<A, A> times(int maxLoops) {
        return __.<A>start().times(maxLoops);
    }

    /**
     * Specifies the condition to stop repeating a traversal.
     *
     * @param <A> the type of the traversal
     * @param untilTraversal the traversal that determines when to stop
     * @return a traversal that stops when the condition is met
     */
    public static <A> GraphTraversal<A, A> until(final Traversal<?, ?> untilTraversal) {
        return __.<A>start().until(untilTraversal);
    }

    /**
     * Emits intermediate results during traversal.
     *
     * @param <A> the type of the traversal
     * @return a traversal that emits intermediate results
     */
    public static <A> GraphTraversal<A, A> emit() {
        return __.<A>start().emit();
    }

    /**
     * Emits intermediate results based on a condition.
     *
     * @param <A> the type of the traversal
     * @param emitTraversal the traversal that determines when to emit
     * @return a traversal that emits results based on the condition
     */
    public static <A> GraphTraversal<A, A> emit(Traversal<?, ?> emitTraversal) {
        return __.<A>start().emit(emitTraversal);
    }

    /**
     * Filters elements using a glob pattern.
     *
     * @param <A> the type of the traversal
     * @param pattern the glob pattern to match against
     * @return a traversal filtered by the glob pattern
     */
    public static <A> GraphTraversal<A, JsonObject> glob(String pattern) {
        return __.<A>start().glob(pattern);
    }

}
