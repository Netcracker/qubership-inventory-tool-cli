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

import org.qubership.itool.modules.gremlin2.util.Compare;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

public class P<V> implements Predicate<V> {

    protected BiPredicate<V, V> biPredicate;
    protected V value;
    protected V originalValue;

    protected P() {

    }

    protected P(BiPredicate<V, V> biPredicate, V originalValue, V value) {
        this.biPredicate = biPredicate;
        this.value = value;
        this.originalValue = originalValue;
    }

    public P(BiPredicate<V, V> biPredicate, V value) {
        this.value = value;
        this.originalValue = value;
        this.biPredicate = biPredicate;
    }

    @Override
    public boolean test(V testValue) {
        return this.biPredicate.test(testValue, (this.originalValue != null) ? this.originalValue : this.value);
    }

    public V getValue() {
        return this.value;
    }

    public void setValue(final V value) {
        this.value = value;
    }

    public BiPredicate<V, V> getBiPredicate() {
        return biPredicate;
    }

    public void setBiPredicate(BiPredicate<V, V> biPredicate) {
        this.biPredicate = biPredicate;
    }

    public V getOriginalValue() {
        return originalValue;
    }

    public void setOriginalValue(V originalValue) {
        this.originalValue = originalValue;
    }

    @Override
    public P<V> clone() {
        P<V> clone = new P(this.biPredicate, this.originalValue, this.value);
        return clone;
    }

    // =======================================================================================

    public static <V> P<V> eq(V value) {
        return new P(Compare.eq, value);
    }

    public static <V> P<V> neq(V value) {
        return new P(Compare.neq, value);
    }

    // XXX within does not conform BiPredicate<V, V>
    public static <V> P<V> within(List<V> value) {
        return new P(Compare.within, value);
    }

    public static <V> P<V> within(V ... values) {
        return new P(Compare.within, Arrays.asList(values));
    }

    // XXX without does not conform BiPredicate<V, V>
    public static <V> P<V> without(V ... values) {
        return new P(Compare.without, Arrays.asList(values));
    }

    public static <V> P<V> exists() {
        return new P(Compare.exists, null);
    }

    public static <V> P<V> isNull() {
        return new P(Compare.isNull, null);
    }

    public static <V> P<V> containing(V value) {
        return new P(Compare.containing, value);
    }

    public static <V> P<V> notContaining(V value) {
        return new P(Compare.notContaining, value);
    }

    public static <V> P<V> startingWith(V value) {
        return new P(Compare.startingWith, value);
    }

    public static <V> P<V> notStartingWith(V value) {
        return new P(Compare.notStartingWith, value);
    }

    public static <V> P<V> endingWith(V value) {
        return new P(Compare.endingWith, value);
    }

    public static <V> P<V> notEndingWith(V value) {
        return new P(Compare.notEndingWith, value);
    }

    public static <V> P<V> lt(V value) {
        return new P(Compare.lt, value);
    }

    public static <V> P<V> lte(V value) {
        return new P(Compare.lte, value);
    }

    public static <V> P<V> gt(V value) {
        return new P(Compare.gt, value);
    }

    public static <V> P<V> gte(V value) {
        return new P(Compare.gte, value);
    }

    public static <V> P<V> lteVersion(V value) {
        return new P(Compare.lteVersion, value);
    }

    public static <V> P<V> gteVersion(V value) {
        return new P(Compare.gteVersion, value);
    }

    public static <V> P<V> inside(final V left, final V right) {
        return new Inside(left, right);
    }

    static class Inside<V> extends P<V> {
        private V left;
        private V right;

        public Inside(V left, V right) {
            this.left = left;
            this.right = right;
        }

        @Override
        public boolean test(V v) {
            P pLeft = new P(Compare.gte, left);
            P pRight = new P(Compare.lte, right);
            return pLeft.test(v) && pRight.test(v);
        }

        @Override
        public String toString() {
            return "inside(" + left + "," + right + ")";
        }

    }
}
