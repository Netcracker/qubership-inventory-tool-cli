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

package org.qubership.itool.modules.gremlin2.util;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.qubership.itool.modules.gremlin2.GremlinException;
import org.qubership.itool.utils.JsonUtils;

public final class ValueHelper {

    public static final Pattern PATTERN = Pattern.compile("^/([^/]+)/?(.+)?$");

    public static String getPropertyKey(String valueKey) {
        if (valueKey.contains(":")) {
            return valueKey.substring(0, valueKey.indexOf(":"));
        } else if (valueKey.startsWith("/")) {
            return valueKey.substring(valueKey.lastIndexOf("/") + 1);
        }
        return valueKey;
    }

    public static Object getObjectValue(String valueKey, Object obj) {
        int indexOf = valueKey.indexOf(":");
        if (indexOf != -1) {
            valueKey = valueKey.substring(indexOf + 1);
        }

        if (valueKey.equals("/")) { // Let apply pointer "/" to scalars and Lists
            return obj;
        }
        JsonObject json = JsonUtils.asJsonObject(obj);  // Throws CCE for anything except Map or JsonObject

        if (valueKey.startsWith("/")) {
            return getObjectFromJsonPointer(json, valueKey);
        } else {
            return json.getValue(valueKey);
        }
    }

    // Like JsonPointer, but supports "[]"'s, producing List of specific results
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static Object getObjectFromJsonPointer(Object obj, String jsonKey) {
        if (! jsonKey.contains("[]")) {
            JsonPointer ptr = JsonPointer.from(jsonKey);
            return ptr.queryJson(obj);
        }

        Object result = obj;
        String[] array = jsonKey.split("/");
        for (int i = 0 ; i < array.length ; i++) {
            String item = array[i];
            if ("".equals(item)) {
                continue;
            }

            if (item.endsWith("[]")) {
                String key = item.substring(0, item.length() - 2);
                Object tmp = ((JsonObject)result).getValue(key);
                if (tmp == null) {
                    return null; // nothing found
                }
                if (tmp instanceof JsonArray) {
                    result = new ArrayList<>();
                    String newKey = keyFor(array, i + 1);
                    for (Object a : (JsonArray) tmp) {
                        ((List)result).add(getObjectFromJsonPointer(a, newKey));
                    }
                    break;  // break 'for' over array

                } else {
                    throw new GremlinException("Value with key: '" + key + "' should be JsonArray");
                }

            } else if (result instanceof JsonObject) {
                result = ((JsonObject)result).getValue(item);

            } else if (result instanceof JsonArray && item.matches("\\d+")) {
                JsonArray r = ((JsonArray)result);
                int index = Integer.parseInt(item);
                if (index < r.size()) {
                    result = r.getValue(index);
                } else {
                    result = null;
                }

            } else {
                result = null;
            }

            if (result == null) {
                return null;    // break 'for' over array
            }
        }
        return result;
    }

    private static String keyFor(String[] array, int pos) {
        StringBuilder builder = new StringBuilder();
        for (int i = pos ; i < array.length ; i++) {
            builder.append("/").append(array[i]);
        }
        return builder.toString();
    }

}
