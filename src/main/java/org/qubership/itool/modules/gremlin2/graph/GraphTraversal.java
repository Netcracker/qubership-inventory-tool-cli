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
import org.qubership.itool.modules.gremlin2.Step;
import org.qubership.itool.modules.gremlin2.Traversal;
import org.qubership.itool.modules.gremlin2.graph.lambda.TrueTraversal;
import org.qubership.itool.modules.gremlin2.step.ByModulating;
import org.qubership.itool.modules.gremlin2.step.TimesModulating;
import org.qubership.itool.modules.gremlin2.step.barrier.CountGlobalStep;
import org.qubership.itool.modules.gremlin2.step.barrier.FoldStep;
import org.qubership.itool.modules.gremlin2.step.barrier.GroupStep;
import org.qubership.itool.modules.gremlin2.step.barrier.OrderStep;
import org.qubership.itool.modules.gremlin2.step.barrier.ProfileStep;
import org.qubership.itool.modules.gremlin2.step.barrier.SumStep;
import org.qubership.itool.modules.gremlin2.step.barrier.TreeStep;
import org.qubership.itool.modules.gremlin2.step.branch.ForkJoinStep;
import org.qubership.itool.modules.gremlin2.step.branch.LocalStep;
import org.qubership.itool.modules.gremlin2.step.branch.RepeatStep;
import org.qubership.itool.modules.gremlin2.step.branch.UnionStep;
import org.qubership.itool.modules.gremlin2.step.filter.DedupStep;
import org.qubership.itool.modules.gremlin2.step.filter.EmptyStep;
import org.qubership.itool.modules.gremlin2.step.filter.IsStep;
import org.qubership.itool.modules.gremlin2.step.filter.NoneStep;
import org.qubership.itool.modules.gremlin2.step.filter.NotStep;
import org.qubership.itool.modules.gremlin2.step.filter.OrStep;
import org.qubership.itool.modules.gremlin2.step.filter.RangeLimitStep;
import org.qubership.itool.modules.gremlin2.step.filter.WherePredicateStep;
import org.qubership.itool.modules.gremlin2.step.map.CapStep;
import org.qubership.itool.modules.gremlin2.step.map.SelectStep;
import org.qubership.itool.modules.gremlin2.step.map.flat.CoalesceStep;
import org.qubership.itool.modules.gremlin2.step.map.flat.EdgeStep;
import org.qubership.itool.modules.gremlin2.step.map.flat.EdgeVertexStep;
import org.qubership.itool.modules.gremlin2.step.map.flat.GlobStep;
import org.qubership.itool.modules.gremlin2.step.map.flat.PropertyKeyStep;
import org.qubership.itool.modules.gremlin2.step.map.flat.PropertyValueStep;
import org.qubership.itool.modules.gremlin2.step.map.flat.SplitStep;
import org.qubership.itool.modules.gremlin2.step.map.flat.UnfoldStep;
import org.qubership.itool.modules.gremlin2.step.map.flat.VertexStep;
import org.qubership.itool.modules.gremlin2.step.map.scalar.IdStep;
import org.qubership.itool.modules.gremlin2.step.map.scalar.MapToVertexStep;
import org.qubership.itool.modules.gremlin2.step.map.scalar.NameStep;
import org.qubership.itool.modules.gremlin2.step.map.scalar.PathStep;
import org.qubership.itool.modules.gremlin2.step.map.scalar.SelectScalarStep;
import org.qubership.itool.modules.gremlin2.step.map.scalar.SizeStep;
import org.qubership.itool.modules.gremlin2.step.map.scalar.TypeStep;
import org.qubership.itool.modules.gremlin2.step.map.scalar.ValueMapStep;
import org.qubership.itool.modules.gremlin2.step.map.scalar.ValueReplaceStep;
import org.qubership.itool.modules.gremlin2.step.map.scalar.ValueStep;
import org.qubership.itool.modules.gremlin2.step.sideEffect.SubgraphStep;
import org.qubership.itool.modules.gremlin2.step.util.HasContainer;
import org.qubership.itool.modules.gremlin2.step.util.Profile;
import org.qubership.itool.modules.gremlin2.step.util.Tree;
import org.qubership.itool.modules.gremlin2.structure.Direction;
import org.qubership.itool.modules.gremlin2.structure.MapElement;
import org.qubership.itool.modules.gremlin2.util.Order;
import org.qubership.itool.modules.gremlin2.util.TraversalHelper;
import io.vertx.core.json.JsonObject;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ForkJoinPool;

import static org.qubership.itool.modules.graph.Graph.F_ID;

public interface GraphTraversal<S, E> extends Traversal<S, E> {

    default Profile profile() {
        this.asAdmin().addStep(new ProfileStep(this.asAdmin()));
        return (Profile)this.next();
    }

    default GraphTraversal<S, E> none() {
        return (GraphTraversal)this.asAdmin().addStep(new NoneStep<>(this.asAdmin()));
    }

    default GraphTraversal<S, E> empty() {
        return (GraphTraversal)this.asAdmin().addStep(new EmptyStep<>(this.asAdmin()));
    }

    default <E2> GraphTraversal<S, E2> local(Traversal<?, E2> localTraversal) {
        return (GraphTraversal)this.asAdmin().addStep(
            new LocalStep<>(this.asAdmin(), localTraversal.asAdmin()));
    }

    default <E2> GraphTraversal<S, E2> fork(Traversal<?, E2> forkedTraversal) {
        return fork(ForkJoinPool.commonPool(), forkedTraversal);
    }

    default <E2> GraphTraversal<S, E2> fork(ForkJoinPool pool, Traversal<?, E2> forkedTraversal) {
        return (GraphTraversal)this.asAdmin().addStep(
            new ForkJoinStep<>(this.asAdmin(), forkedTraversal, pool)
        );
    }

    default GraphTraversal<S, E> mapToVertex() {
        return (GraphTraversal)this.asAdmin().addStep(new MapToVertexStep<>(this.asAdmin()));
    }

    default GraphTraversal<S, E> hasId(String ... ids) {
        List<String> idList = Arrays.asList(ids);
        return TraversalHelper
            .addHasContainer(this.asAdmin(), new HasContainer(
                F_ID, idList.size() == 1 ? P.eq(idList.get(0)) : P.within(idList))
            );
    }

    default GraphTraversal<S, E> hasNotId(String ... ids) {
        for (String id : ids) {
            TraversalHelper.addHasContainer(this.asAdmin(), new HasContainer(
                F_ID, P.neq(id)
            ));
        }
        return this;
    }

    /** Has ALL listed keys (empty array = accept all)
     * @param keys Keys to check
     * @return Traversal
     */
    default GraphTraversal<S, E> hasKey(String ... keys) {
        for (String key : keys) {
            TraversalHelper.addHasContainer(this.asAdmin(), new HasContainer(
                key, P.exists()
            ));
        }
        return this;
    }

    /** Has ALL listed keys (empty array = accept all)
     * @param keys Keys to check
     * @return Traversal
     */
    default GraphTraversal<S, E> hasKeys(String ... keys) {
        return hasKey(keys);
    }

    /** Has NONE of listed keys (empty array = accept all)
     * @param keys Keys to check
     * @return Traversal
     */
    default GraphTraversal<S, E> hasNot(String ... keys) {
        for (String key : keys) {
            TraversalHelper.addHasContainer(this.asAdmin(), new HasContainer(
                key, P.isNull()
            ));
        }
        return this;
    }

    /** Has ANY of listed types (empty array = accept nothing)
     * @param types Types to check
     * @return Traversal
     */
    default GraphTraversal<S, E> hasType(String ... types) {
        List<String> typeList = Arrays.asList(types);
        return TraversalHelper
            .addHasContainer(this.asAdmin(), new HasContainer(
                "type", typeList.size() == 1 ? P.eq(typeList.get(0)) : P.within(typeList))
        );
    }

    /** Has ANY of listed types (empty array = accept nothing)
     * @param types Types to check
     * @return Traversal
     */
    default GraphTraversal<S, E> hasTypes(String ... types) {
        return hasType(types);
    }

    default GraphTraversal<S, E> has(String type, String propertyKey, String value) {
        return TraversalHelper
            .addHasContainer(this.asAdmin(), type, new HasContainer(propertyKey, P.eq(value)));
    }

    default GraphTraversal<S, E> has(String type, String propertyKey, P<?> predicate) {
        return TraversalHelper
            .addHasContainer(this.asAdmin(), type, new HasContainer(propertyKey, predicate));
    }

    default GraphTraversal<S, E> has(String propertyKey, String value) {
        return TraversalHelper
            .addHasContainer(this.asAdmin(), new HasContainer(propertyKey, P.eq(value)));
    }

    default GraphTraversal<S, E> has(String propertyKey, P<?> predicate) {
        return TraversalHelper
            .addHasContainer(this.asAdmin(), new HasContainer(propertyKey, predicate));
    }

    default GraphTraversal<S, String> id() {
        return this.asAdmin().addStep(
            new IdStep<>(this.asAdmin())
        );
    }

    default GraphTraversal<S, String> type() {
        return this.asAdmin().addStep(
            new TypeStep<>(this.asAdmin())
        );
    }

    default GraphTraversal<S, String> name() {
        return this.asAdmin().addStep(
            new NameStep<>(this.asAdmin())
        );
    }

    default GraphTraversal<S, E> key() {
        return (GraphTraversal<S, E>) this.asAdmin().addStep(
            new PropertyKeyStep<>(this.asAdmin())
        );
    }

    default GraphTraversal<S, E> value() {
        return (GraphTraversal<S, E>) this.asAdmin().addStep(
            new PropertyValueStep<>(this.asAdmin())
        );
    }

    default GraphTraversal<S, E> valueReplace(String regex, String replacement) {
        return (GraphTraversal<S, E>) this.asAdmin().addStep(
            new ValueReplaceStep<>(this.asAdmin(), regex, replacement)
        );
    }

    default GraphTraversal<S, Integer> size() {
        return this.asAdmin().addStep(
            new SizeStep<>(this.asAdmin())
        );
    }

    default GraphTraversal<S, Tree> tree() {
        return this.asAdmin().addStep(
            new TreeStep<>(this.asAdmin())
        );
    }

    default GraphTraversal<S, E> as(String stepLabel, String... stepLabels) {
        Step<?, E> endStep = this.asAdmin().getEndStep();
        endStep.addLabel(stepLabel);
        for (final String label : stepLabels) {
            endStep.addLabel(label);
        }
        return this;
    }

    default <E> GraphTraversal<S, E> select(String selectKey) {
        Traversal.Admin<S, Map<String, Object>> sMapAdmin = this.asAdmin().addStep(
            new SelectScalarStep<>(this.asAdmin(), selectKey)
        );
        return (GraphTraversal) sMapAdmin;
    }

    default <E> GraphTraversal<S, E> cap(String sideEffectKey) {
        Traversal.Admin<S, Map<String, Object>> sMapAdmin = this.asAdmin().addStep(
            new CapStep<>(this.asAdmin(), sideEffectKey)
        );
        return (GraphTraversal) sMapAdmin;
    }

    default <E2> GraphTraversal<S, Map<String, E2>> select(String... selectKeys) {
        Traversal.Admin<S, Map<String, Object>> sMapAdmin = this.asAdmin().addStep(
            new SelectStep<>(this.asAdmin(), selectKeys)
        );
        return (GraphTraversal) sMapAdmin;
    }

    default GraphTraversal<S, Path> path() {
        return this.asAdmin().addStep(
            new PathStep<>(this.asAdmin())
        );
    }

    // Note: this Gremlin method produces multiple traverses if there exist multiple edges between this vertex and that one
    default GraphTraversal<S, JsonObject> out(String... edgeLabels) {
        return this.asAdmin().addStep(
            new VertexStep<>(this.asAdmin(), Direction.OUT, edgeLabels)
        );
    }

    // Note: this Gremlin method produces multiple traverses if there exist multiple edges between this vertex and that one
    default GraphTraversal<S, JsonObject> in(String... edgeLabels) {
        return this.asAdmin().addStep(
            new VertexStep<>(this.asAdmin(), Direction.IN, edgeLabels)
        );
    }

    // Note: this Gremlin method produces multiple traverses if there exist multiple edges between this vertex and that one
    default GraphTraversal<S, JsonObject> both(String... edgeLabels) {
        return this.asAdmin().addStep(
            new VertexStep<>(this.asAdmin(), Direction.BOTH, edgeLabels)
        );
    }

    default GraphTraversal<S, JsonObject> outE(String... edgeLabels) {
        return this.asAdmin().addStep(
            new EdgeStep<>(this.asAdmin(), Direction.OUT, edgeLabels)
        );
    }

    default GraphTraversal<S, JsonObject> inE(String... edgeLabels) {
        return this.asAdmin().addStep(
            new EdgeStep<>(this.asAdmin(), Direction.IN, edgeLabels)
        );
    }

    default GraphTraversal<S, JsonObject> bothE(String... edgeLabels) {
        return this.asAdmin().addStep(
            new EdgeStep<>(this.asAdmin(), Direction.BOTH, edgeLabels)
        );
    }

    default GraphTraversal<S, JsonObject> inV() {
        return this.asAdmin().addStep(
            new EdgeVertexStep(this.asAdmin(), Direction.IN)
        );
    }

    default GraphTraversal<S, JsonObject> outV() {
        return this.asAdmin().addStep(
            new EdgeVertexStep(this.asAdmin(), Direction.OUT)
        );
    }

    default GraphTraversal<S, JsonObject> bothV() {
        return this.asAdmin().addStep(
            new EdgeVertexStep(this.asAdmin(), Direction.BOTH)
        );
    }

    default <E2> GraphTraversal<S, E2> value(String propertyKey) {
        return (GraphTraversal)this.asAdmin().addStep(
            new ValueStep<>(this.asAdmin(), propertyKey)
        ) ;
    }

    default <E2> GraphTraversal<S, Map<Object, E2>> values(String... propertyKeys) {
        return (GraphTraversal)this.asAdmin().addStep(
            new ValueMapStep<>(this.asAdmin(), propertyKeys)
        );
    }

    default GraphTraversal<S, Long> count() {
        return this.asAdmin().addStep(new CountGlobalStep<>(this.asAdmin()));
    }

    default GraphTraversal<S, Float> sum() {
        return this.asAdmin().addStep(new SumStep(this.asAdmin()));
    }

    default <K, V> GraphTraversal<S, Map<K, V>> group() {
        return (GraphTraversal)this.asAdmin().addStep(new GroupStep<>(this.asAdmin()));
    }

    default GraphTraversal<S, E> by(String string) {
        Step<?, E> endStep = this.asAdmin().getEndStep();
        if (endStep instanceof ByModulating) {
            ByModulating byModulating = (ByModulating) endStep;
            byModulating.modulateBy(string);
        } else {
            throw new UnsupportedOperationException("Step " + endStep.getClass().getSimpleName() + " not supported .by() modulation");
        }
        return this;
    }

    default GraphTraversal<S, E> by(String ... args) {
        Step<?, E> endStep = this.asAdmin().getEndStep();
        if (endStep instanceof ByModulating) {
            ByModulating byModulating = (ByModulating) endStep;
            byModulating.modulateBy(args);
        } else {
            throw new UnsupportedOperationException("Step " + endStep.getClass().getSimpleName() + " not supported .by() modulation");
        }
        return this;
    }

    default GraphTraversal<S, E> by(MapElement mapElement) {
        Step<?, E> endStep = this.asAdmin().getEndStep();
        if (endStep instanceof ByModulating) {
            ByModulating byModulating = (ByModulating) endStep;
            byModulating.modulateBy(mapElement);
        } else {
            throw new UnsupportedOperationException("Step " + endStep.getClass().getSimpleName() + " not supported .by() modulation");
        }
        return this;
    }

    default GraphTraversal<S, E> by(Traversal traversal) {
        Step<?, E> endStep = this.asAdmin().getEndStep();
        if (endStep instanceof ByModulating) {
            ByModulating byModulating = (ByModulating) endStep;
            byModulating.modulateBy(traversal);
        } else {
            throw new UnsupportedOperationException("Step " + endStep.getClass().getSimpleName() + " not supported .by() modulation");
        }
        return this;
    }

    default GraphTraversal<S, E> by(Order order) {
        Step<?, E> endStep = this.asAdmin().getEndStep();
        if (endStep instanceof ByModulating) {
            ByModulating byModulating = (ByModulating) endStep;
            byModulating.modulateBy(order);
        } else {
            throw new UnsupportedOperationException("Step " + endStep.getClass().getSimpleName() + " not supported .by() modulation");
        }
        return this;
    }

    default GraphTraversal<S, E> by(String propertyKey, Order order) {
        Step<?, E> endStep = this.asAdmin().getEndStep();
        if (endStep instanceof ByModulating) {
            ByModulating byModulating = (ByModulating) endStep;
            byModulating.modulateBy(propertyKey, order);
        } else {
            throw new UnsupportedOperationException("Step " + endStep.getClass().getSimpleName() + " not supported .by() modulation");
        }
        return this;
    }

    default <E2> GraphTraversal<S, E2> coalesce(Traversal<?, E2>... coalesceTraversals) {
        return (GraphTraversal)this.asAdmin().addStep(
            new CoalesceStep<>(this.asAdmin(), Arrays.copyOf(coalesceTraversals, coalesceTraversals.length, Traversal.Admin[].class))
        );
    }

    default GraphTraversal<S, E> is(Object value) {
        return this.asAdmin().addStep(
            new IsStep<>(this.asAdmin(), value instanceof P ? (P<E>) value : P.eq((E) value))
        );
    }

    default GraphTraversal<S, E> where(String startKey, P<String> predicate) {
        return (GraphTraversal)this.asAdmin().addStep(
            new WherePredicateStep<>(this.asAdmin(), Optional.ofNullable(startKey), predicate)
        );
    }

    default GraphTraversal<S, E> where(P<String> predicate) {
        return (GraphTraversal)this.asAdmin().addStep(
            new WherePredicateStep<>(this.asAdmin(), Optional.empty(), predicate)
        );
    }

    default GraphTraversal<S, E> where(Traversal<?, ?> innerTraversal) {
        return (GraphTraversal)this.asAdmin().addStep(
            new WherePredicateStep<>(this.asAdmin(), innerTraversal)
        );
    }

    default GraphTraversal<S, E> dedup() {
        return (GraphTraversal)this.asAdmin().addStep(
            new DedupStep<>(this.asAdmin())
        );
    }

    default <E2> GraphTraversal<S, E2> union(Traversal<?, E2> ... unionTraversals) {
        return (GraphTraversal)this.asAdmin().addStep(
            new UnionStep<S, E2>(this.asAdmin(), Arrays.copyOf(unionTraversals, unionTraversals.length, Traversal.Admin[].class))
        );
    }

    default GraphTraversal<S, E> not(Traversal<?, ?> innerTraversal) {
        return (GraphTraversal)this.asAdmin().addStep(
            new NotStep<>(this.asAdmin(), innerTraversal)
        );
    }

    default <E2> GraphTraversal<S, E2> or(final Traversal<?, E2> ... orTraversals) {
        return (GraphTraversal)this.asAdmin().addStep(
            new OrStep<E2>(this.asAdmin(), Arrays.copyOf(orTraversals, orTraversals.length, Traversal.Admin[].class))
        );
    }

    default GraphTraversal<S, E> range(int rangeFrom, int rangeTo) {
        return (GraphTraversal)this.asAdmin().addStep(
            new RangeLimitStep<>(this.asAdmin(), Optional.of(rangeFrom), Optional.of(rangeTo))
        );
    }

    default GraphTraversal<S, E> limit(int limit) {
        return (GraphTraversal)this.asAdmin().addStep(
            new RangeLimitStep<>(this.asAdmin(), Optional.empty(), Optional.of(limit))
        );
    }

    default GraphTraversal<S, E> tail(int rangeFrom) {
        return (GraphTraversal)this.asAdmin().addStep(
            new RangeLimitStep<>(this.asAdmin(), Optional.of(rangeFrom), Optional.empty())
        );
    }

    default GraphTraversal<S, E> order() {
        return (GraphTraversal)this.asAdmin().addStep(new OrderStep<>(this.asAdmin()));
    }

    default <E2 extends String> GraphTraversal<String, String> split() {
        return (GraphTraversal)this.asAdmin().addStep(new SplitStep<>(this.asAdmin()));
    }

    default <E2> GraphTraversal<S, E2> unfold() {
        return (GraphTraversal)this.asAdmin().addStep(new UnfoldStep<>(this.asAdmin()));
    }

    default GraphTraversal<S, List<E>> fold() {
        return (GraphTraversal)this.asAdmin().addStep(new FoldStep<>(this.asAdmin()));
    }

    default GraphTraversal<S, JsonObject> subgraph(String sideEffectKey) {
        return this.asAdmin().addStep(new SubgraphStep(this.asAdmin(), sideEffectKey));
    }

    default GraphTraversal<S, E> repeat(Traversal<?, E> repeatTraversal) {
        return RepeatStep.addRepeatToTraversal(this, (Traversal.Admin<E, E>) repeatTraversal);
    }

    default GraphTraversal<S, E> times(int maxLoops) {
        if (this.asAdmin().getEndStep() instanceof TimesModulating) {
            ((TimesModulating) this.asAdmin().getEndStep()).modulateTimes(maxLoops);
            return this;
        } else {
            throw new IllegalStateException("times() modulator supported only for TimesModulating implementations");
//            return RepeatStep.addUntilToTraversal(this, new LoopTraversal<>(maxLoops));
        }
    }

    default GraphTraversal<S, E> until(final Traversal<?, ?> untilTraversal) {
        return RepeatStep.addUntilToTraversal(this, (Traversal.Admin<E, ?>) untilTraversal);
    }

    default GraphTraversal<S, E> emit() {
        return RepeatStep.addEmitToTraversal(this, TrueTraversal.instance());
    }

    default GraphTraversal<S, E> emit(Traversal<?, ?> emitTraversal) {
        return RepeatStep.addEmitToTraversal(this, (Traversal.Admin<E, ?>) emitTraversal);
    }

    default GraphTraversal<S, JsonObject> glob(String pattern) {
        return this.asAdmin().addStep(
            new GlobStep(this.asAdmin(), pattern)
        );
    }

    /*
    default GraphTraversal<S, E> emit(final Predicate<Traverser<E>> emitPredicate) {
        return RepeatStep.addEmitToTraversal(this, (Traversal.Admin<E, ?>) __.filter(emitPredicate));
    }


    default GraphTraversal<S, E> until(final Predicate<Traverser<E>> untilPredicate) {
        return RepeatStep.addUntilToTraversal(this, (Traversal.Admin<E, ?>) __.filter(untilPredicate));
    }
    */

    // ======================================================================================
    // ======================================================================================
    // ======================================================================================

    @Override
    GraphTraversal.Admin<S, E> clone();

    default GraphTraversal.Admin<S, E> asAdmin() {
        return (GraphTraversal.Admin<S, E>) this;
    }


    interface Admin<S, E> extends Traversal.Admin<S, E>, GraphTraversal<S, E> {

        @Override
        default <E2> GraphTraversal.Admin<S, E2> addStep(Step<?, E2> step) {
            return (GraphTraversal.Admin<S, E2>) Traversal.Admin.super.addStep((Step) step);
        }

        @Override
        GraphTraversal.Admin<S, E> clone();

        GraphTraversalSource getSource();

        void setSource(GraphTraversalSource source);

    }

}
