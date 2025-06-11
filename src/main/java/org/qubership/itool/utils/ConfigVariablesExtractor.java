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

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConfigVariablesExtractor {

    final static Pattern VALUES_EXTRACTION_PATTERN = Pattern.compile("\\.Values\\.(\\S+)\\s*");
    final static Pattern PREPROCESSOR_EXPRESSION_PATTERN = Pattern.compile("\\{\\{(.+?)\\}\\}");

    public static Set<String> extractVariableNames(final Object jsonStructure) {
        Set<String> collector = new HashSet<>();
        handleObjectContents(collector, jsonStructure);
        return collector;
    }

    private static void handleObjectContents(Set<String> collector, final Object object) {
        Object structure = object;
        if (object instanceof List) {
            structure = new JsonArray((List) structure);
        } else if (object instanceof Map) {
            structure = new JsonObject((Map<String, Object>) object);
        } else if (object instanceof String) {
            handleStringValue(collector, (String) object);
            return;
        }

        if (structure instanceof JsonObject) {
            handleJsonObjectFields(collector, (JsonObject) structure);
        } else if (structure instanceof JsonArray) {
            handleJsonArrayItems(collector, (JsonArray) structure);
        }
    }

    private static void handleJsonArrayItems(Set<String> collector, final JsonArray jsonArrayStructure) {
        for (Object element : jsonArrayStructure) {
            handleObjectContents(collector, element);
        }
    }

    private static void handleJsonObjectFields(Set<String> collector, final JsonObject jsonObjectStructure) {
        for (Map.Entry<String, Object> element : jsonObjectStructure) {
            handleObjectContents(collector, element.getValue());
        }
    }

    private static void handleStringValue(Set<String> collector, final String value) {
        if (value == null || !value.contains(".Values.")) {
            return;
        }
        getVariableNames(collector, getValuesByPattern(value, PREPROCESSOR_EXPRESSION_PATTERN));
    }

    private static void getVariableNames(Set<String> collector, final Set<String> preprocessorExpressions) {
        for (String expression : preprocessorExpressions) {
            collector.addAll(getValuesByPattern(expression, VALUES_EXTRACTION_PATTERN));
        }
    }

    private static Set<String> getValuesByPattern(final String sourceString, final Pattern pattern) {
        Set<String> result = new HashSet<>();
        Matcher matcher = pattern.matcher(sourceString);
        while (matcher.find()) {
            result.add(matcher.group(1));
        }
        return result;
    }

}
