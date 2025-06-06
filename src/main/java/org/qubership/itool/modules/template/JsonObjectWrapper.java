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

package org.qubership.itool.modules.template;

import freemarker.template.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class JsonObjectWrapper extends DefaultObjectWrapper {

    public JsonObjectWrapper(Version incompatibleImprovements) {
        super(incompatibleImprovements);
    }

    @Override
    protected TemplateModel handleUnknownType(final Object obj) throws TemplateModelException {
        if (obj instanceof JsonObject) {
            return this.wrap(((JsonObject) obj).getMap());
        }
        if (obj instanceof JsonArray) {
            return this.wrap(((JsonArray) obj).getList());
        }
        return super.handleUnknownType(obj);
    }
}
