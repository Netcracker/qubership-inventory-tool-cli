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

package org.qubership.itool.modules.graph;

import io.vertx.core.json.JsonObject;

import java.util.LinkedHashSet;
import java.util.Set;

public class VertexImpl implements Vertex {
    private String id;
    private JsonObject value;
    private Set<String> edgesIn;
    private Set<String> edgesOut;

    public VertexImpl(String id, JsonObject value) {
        this.id = id;
        this.value = value;
        this.edgesIn = new LinkedHashSet<>();
        this.edgesOut = new LinkedHashSet<>();
    }

    @Override
    public void setId(String newId) {
        id = newId;
        value.put(Graph.F_ID, newId);
    }

    @Override
    public Set<String> getEdgesIn() {
        return edgesIn;
    }

    @Override
    public Set<String> getEdgesOut() {
        return edgesOut;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public JsonObject getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "Vertex{" +
            "id=" + id +
            ", value=" + value +
            ", edgesIn=" + edgesIn +
            ", edgesOut=" + edgesOut +
            '}';
    }

}
