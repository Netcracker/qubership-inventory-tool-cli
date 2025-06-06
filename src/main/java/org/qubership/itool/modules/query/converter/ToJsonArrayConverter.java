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

package org.qubership.itool.modules.query.converter;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.Map;
import java.util.Properties;

public class ToJsonArrayConverter implements ResultConverter<JsonArray> {

    @SuppressWarnings("unused")
    private Properties props;

    @Override
    public void setProperties(Properties properties) {
        this.props = properties;
    }

    @Override
    public String supportType() {
        return "json";
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public JsonArray convert(Object gremlinResult) {
        JsonArray result = new JsonArray();

        // XXX Currently, only List supported. See ToTextConverter.convert() for other possible result types
        List<?> objList = (List)gremlinResult;
        for (Object obj : objList) {
            if (obj instanceof Map) {
                result.add(new JsonObject((Map)obj));
            } else {
                result.add(obj);
            }
        }

        return result;
    }
}
