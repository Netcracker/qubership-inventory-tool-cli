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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DefaultPath implements Path {

    protected List objects;
    protected List<Set<String>> labels;

    public DefaultPath() {
        this.objects = new ArrayList();
        this.labels = new ArrayList<>();
    }

    public DefaultPath(List objects, List<Set<String>> labels) {
        this.objects = objects;
        this.labels = labels;
    }

    @Override
    public List<Set<String>> labels() {
        return this.labels;
    }

    @Override
    public List objects() {
        return this.objects;
    }

    @Override
    public Path extend(Object obj) {
        this.objects.add(obj);
        this.labels.add(new HashSet<>());
        return this;
    }

    @Override
    public Path extend(Object obj, String... labels) {
        this.objects().add(obj);
        this.labels().add(new HashSet<>());
        extend(labels);
        return this;
    }

    @Override
    public Path extend(Object obj, Set<String> labels) {
        this.objects().add(obj);
        this.labels().add(new HashSet<>());
        extend(labels);
        return this;
    }

    @Override
    public Path extend(String... labels) {
        if (labels == null || this.labels.size() == 0) {
            return this;
        }
        Set<String> set = this.labels.get(this.labels.size() - 1);
        for (String label : labels) {
            set.add(label);
        }
        return this;
    }

    @Override
    public Path extend(Set<String> labels) {
        if (labels == null || this.labels.size() == 0) {
            return this;
        }
        Set<String> set = this.labels.get(this.labels.size() - 1);
        set.addAll(labels);
        return this;
    }

    @Override
    public Path clone() {
        List cObjects = new ArrayList(this.objects);

        int size = this.labels.size();
        List<Set<String>> cLabels = new ArrayList<>(size * 3);
        for (int i=0 ; i<size ; i++) {
            Set<String> labelSet = this.labels.get(i);
            Set<String> cLabelSet = new HashSet<>(labelSet);
            cLabels.add(cLabelSet);
        }

        Path clone = new DefaultPath(cObjects, cLabels);
        return clone;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Path{\n");
        for (int i=0 ; i<this.objects.size() ; i++) {
            builder.append(i+1).append(") ");
            builder.append(this.labels.get(i)).append(" : ");
            builder.append(this.objects.get(i)).append("\n");
        }
        builder.append("}\n");
        return builder.toString();
    }

}
