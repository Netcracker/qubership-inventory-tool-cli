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

package org.qubership.itool.utils;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class JsonExtractor {

    public static JsonArray getJsonArrayFromStructured(Object structured, String fieldName) {
        if (structured instanceof JsonArray) {
            return getJsonArrayFromJsonArray((JsonArray) structured, fieldName);
        } else if (structured instanceof JsonObject) {
            return getJsonArrayFromJsonObject((JsonObject) structured, fieldName);
        }
        return null;
    }

    private static JsonArray getJsonArrayFromJsonObject(JsonObject jsonObject, String fieldName) {
        return jsonObject.getJsonArray(fieldName);
    }

    private static JsonArray getJsonArrayFromJsonArray(JsonArray jsonArray, String fieldName) {
        return jsonArray.stream()
                .filter(object -> ((JsonObject) object).containsKey(fieldName))
                .map(object -> ((JsonObject) object).getJsonArray(fieldName))
                .findFirst()
                .orElse(null);

    }

    public static JsonArray getStructuredValueFromFile(Object structured) {
        if (structured instanceof JsonArray) {
            return (JsonArray) structured;
        } else if (structured instanceof JsonObject) {
            return new JsonArray().add(structured);
        }
        return null;
    }
}