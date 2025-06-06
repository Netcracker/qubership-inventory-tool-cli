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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

public interface Path extends Cloneable, Iterable<Object> {

    /**
     * An ordered list of the labels associated with the path The set of labels for a particular step are ordered by the order in which extend(Object, Set) was called.
     * @return list of labels
     */
    List<Set<String>> labels();

    /**
     * An ordered list of the objects in the path.
     * @return list of objects
     */
    List objects();

    /**
     * Add a new step to the path.
     * @param obj new step
     * @return path
     */
    Path extend(Object obj);

    /**
     * Add a new step to the path with an object and any number of associated labels.
     * @param obj new step
     * @param labels array of labels
     * @return path
     */
    Path extend(Object obj, String ... labels);
    Path extend(Object obj, Set<String> labels);

    /**
     * Add labels to the head of the path.
     * @param labels array of labels
     * @return path
     */
    Path extend(String ... labels);
    Path extend(Set<String> labels);

    /**
     * Get the object associated with the specified index into the path.
     * @param <A> return type
     * @param index index
     * @return object
     */
    default <A> A get(int index) {
        return (A) this.objects().get(index);
    }

    /**
     * Get the object associated with the particular label of the path.
     * @param <A> return type
     * @param label label of the path
     * @return object
     */
    default <A> A get(String label) {
        final List<Object> objects = this.objects();
        final List<Set<String>> labels = this.labels();
        Object result = null;
        for (int i=0 ; i<labels.size() ; i++) {
            if (!labels.get(i).contains(label)) {
                continue;
            }
            if (result == null) {
                result = objects.get(i);
            } else if (result instanceof List) {
                ((List)result).add(objects.get(i));
            } else {
                List tmp = new ArrayList();
                tmp.add(result);
                tmp.add(objects.get(i));
                result = tmp;
            }
        }
        if (result == null) {
            throw new GremlinException("Step with provided label does not exist. Label: " + label);
        }
        return (A) result;
    }

    /**
     * Return true if the path has the specified label, else return false.
     * @param label path label
     * @return true if the path has the specified label, false otherwise
     */
    default boolean hasLabel(String label) {
        return this.labels().stream().filter(labels -> labels.contains(label)).findAny().isPresent();
    }

    /**
     * Get the head of the path.
     * @return the head of the path
     */
    default Object head() {
        return this.objects().get(this.size() - 1);
    }

    /**
     * Determine if the path is empty or not.
     * @return true if path is empty, false otherwise
     */
    default boolean isEmpty() {
        return this.objects().isEmpty();
    }

    /**
     * Get the number of steps in the path.
     * @return number of steps in the path
     */
    default int size() {
        return this.objects().size();
    }

    @SuppressWarnings("CloneDoesntDeclareCloneNotSupportedException")
    Path clone();


    @Override
    default Iterator<Object> iterator() {
        return this.objects().iterator();
    }

    default void forEach(BiConsumer<Object, Set<String>> consumer) {
        final List<Object> objects = this.objects();
        final List<Set<String>> labels = this.labels();
        for (int i = 0; i < objects.size(); i++) {
            consumer.accept(objects.get(i), labels.get(i));
        }
    }

}
