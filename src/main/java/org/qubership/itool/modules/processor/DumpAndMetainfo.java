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

package org.qubership.itool.modules.processor;

import io.vertx.core.json.JsonObject;

public class DumpAndMetainfo {

    JsonObject dump;
    JsonObject meta;

    public DumpAndMetainfo() {
    }

    /**
     * Create and fill DumpAndMetainfo instance
     *
     * @param dump Dump as JSON model
     * @param meta Description of dump provenance using attributes defined in {@link MergerApi}
     */
    public DumpAndMetainfo(JsonObject dump, JsonObject meta) {
        this.dump = dump;
        this.meta = meta;
    }

    public JsonObject getDump() {
        return dump;
    }

    public DumpAndMetainfo setDump(JsonObject dump) {
        this.dump = dump;
        return this;
    }

    public JsonObject getMeta() {
        return meta;
    }

    public DumpAndMetainfo setMeta(JsonObject meta) {
        this.meta = meta;
        return this;
    }

}
