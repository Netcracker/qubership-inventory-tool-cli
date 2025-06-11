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

public class EdgeImpl implements Edge {
    String id;
    String sourceVertexId;
    String destinationVertexId;
    JsonObject value;

    public EdgeImpl(String id, String sourceVertexId, String destinationVertexId, JsonObject value) {
        this.id = id;
        this.sourceVertexId = sourceVertexId;
        this.destinationVertexId = destinationVertexId;
        this.value = value;
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
    public String getSourceVertexId() {
        return sourceVertexId;
    }

    @Override
    public void setSourceVertexId(String sourceVertexId) {
        this.sourceVertexId = sourceVertexId;
    }

    @Override
    public String getDestinationVertexId() {
        return destinationVertexId;
    }

    @Override
    public void setDestinationVertexId(String destinationVertexId) {
        this.destinationVertexId = destinationVertexId;
    }

    @Override
    public String toString() {
        return "Edge{" +
            "id=" + id +
            ", from=" + sourceVertexId +
            ", to=" + destinationVertexId +
            ", value=" + value +
            '}';
    }

}
