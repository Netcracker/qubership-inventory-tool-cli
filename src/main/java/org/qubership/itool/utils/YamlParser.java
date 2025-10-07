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

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.collect.Streams;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;

public class YamlParser {

    protected static final Logger LOG = LoggerFactory.getLogger(YamlParser.class);

    // When encountering new corner cases, add them to TestYamlParser

    // Quote unquoted values that contain template expressions
    protected static final Pattern VALUE_TEMPLATE_PROTECTOR = Pattern.compile(
        // Key or dash
        "^(\\s*(?:(?!-)[^:]*:|-)?\\s*)"
        // Look-behind: non-quote
        + "(?<![\\'\\\"])"
        // Value with braces in the middle
        + "([\\w-]*\\{\\s*\\{.*?\\}\\s*\\}.*?)\\s*$"
    );

    // Pattern: Handle already-quoted template expressions with nested quotes
    protected static final Pattern NESTED_QUOTE_PROTECTOR = Pattern.compile(
        // Key or dash
        "^(\\s*(?:(?!-)[^:]*:|-)?\\s*)"
        // Opening quote (capture which type)
        + "(['\"])"
        // Template expression with quotes inside
        + "(\\{\\s*\\{.*\\}\\s*\\})"
        // Closing quote (must match opening quote using backreference \2)
        + "(\\2\\s*)$"
    );

    protected static final Pattern AT_PROTECTOR = Pattern.compile(
        // Look-behind: non-quote
        "(^|[^\\'\\\"])"
            // Something between at symbols
            + "(@[^\\s@]+@)"
            // Look-ahead: non-quote
            + "([^\\'\\\"]|$)");
    protected static final String AT_PROTECTOR_REPLACEMENT = "$1'$2'$3";

    // No dash version for a while
    protected static final Pattern SPRING_SUBST_PROTECTOR_KEY = Pattern.compile(
        // $1 = spaces, key, colon, start of value without braces, dollars and quotes
        "^(\\s*[^-:][^:]*:[^\\'\\\"${]*)"
            // $2 = '$' sign, opening brace and everything after the brace
            + "(\\$\\{.*)"
            // $3 = trailing non-space, non-quote
            + "([^\\'\\\"\\s])"
            // trailing spaces (to be omitted)
            + "\\s*$");
//    protected static final String SPRING_SUBST_PROTECTOR_REPLACEMENT = "$1'$2$3'";


    protected static final YAMLFactory defaultYamlFactory = new YAMLFactory();

    protected static final ObjectMapper defaultMapper = new ObjectMapper(defaultYamlFactory)
        .findAndRegisterModules();


    boolean logNormalizedData = true;

    /**
     * Whether result of normalization attempt shall be logged
     *
     * @param logNormalizedData true by default
     */
    public void setLogNormalizedData(boolean logNormalizedData) {
        this.logNormalizedData = logNormalizedData;
    }

    /**
     * Read YAML file (UTF-8 encoding assumed), normalize its data to protect
     * the parser from preprocessing directives, and parse results.
     *
     * @param file What to read
     * @return List of all sections of input file
     * @throws IOException        IO exception
     * @throws JsonParseException Parsing exception
     */
    public List<Object> parseYaml(File file) throws IOException, JsonParseException {
        try (Reader reader = new FileReader(file, JsonUtils.UTF_8)) {
            return parseYaml(reader, file.getAbsolutePath());
        }
    }

    public List<Object> parseYamlData(String yaml, String sourceId) throws IOException, JsonParseException {
        return parseYaml(new StringReader(yaml), sourceId);
    }

    /**
     * Parse YAML data from reader, normalizing input data to protect the parser
     * from preprocessing directives (HELM and others).
     * Buffer input, no need to add more buffering.
     * Automatically closes the input afterwards.
     *
     * @param reader   Input data. Closed after reading.
     * @param sourceId Used for exception reporting
     * @return List of all sections of input file
     * @throws IOException        IO exception
     * @throws JsonParseException Parsing exception
     */
    public List<Object> parseYaml(Reader reader, String sourceId) throws IOException, JsonParseException {
        String normalizedData = null;
        try (LineNumberReader lnr = new LineNumberReader(reader)) {
            normalizedData = lnr.lines()
                .filter(s -> acceptLine(s))
                .map(s -> normalizeLine(s))
                .collect(Collectors.joining("\n", "", "\n"));
            return parseSafeYaml(new StringReader(normalizedData), sourceId);
        } catch (RuntimeException | IOException e) {
            if (logNormalizedData) {
                LOG.warn("Normalized data:\n{}, {}", sourceId, normalizedData);
            }
            throw e;
        }
    }

    public List<Object> parseSafeYaml(Reader reader, String sourceId) throws IOException, JsonParseException {
        try {
// This code produces maps with null keys, according to YAML specs:
//        Yaml yaml = new Yaml();
//        int index = 0;
//        for (Object data: yaml.loadAll(normalizedData)) {
//            processYamlDocument(domain, component, fileName, index++, data);
//        }

// This code produces "~" keys, compatible with JSON model:
            MappingIterator<JsonNode> values = getMapper().readValues(
                getJsonFactory().createParser(reader), JsonNode.class);
            List<Object> subDocuments = Streams.stream(values)
                .map(this::nodeToValue)
                .collect(Collectors.toList());
            return subDocuments;
        } catch (RuntimeException | IOException e) {
            LOG.warn("Failed to parse YAML from {}", sourceId);
            throw e;
        }
    }

    //--- The following methods may be overridden by subclasses to deal with customized YAMLs

    protected boolean acceptLine(String s) {
        String trimmed = s.trim();
        return !trimmed.startsWith("{{") && !trimmed.startsWith("{ {");
    }

    protected String normalizeLine(String s) {

        // Step 1: Quote unquoted values that contain template expressions
        Matcher valueMatcher = VALUE_TEMPLATE_PROTECTOR.matcher(s);
        if (valueMatcher.matches()) {
            return valueMatcher.group(1) + "'" + valueMatcher.group(2) + "'";
        }

        // Step 2: Handle Spring property placeholders (@property@)
        String s1 = AT_PROTECTOR.matcher(s).replaceAll(AT_PROTECTOR_REPLACEMENT);
        if (!s1.equals(s)) {
            return s1;
        }

        // Step 3: Handle Spring substitution placeholders (${property})
        Matcher springMatcher = SPRING_SUBST_PROTECTOR_KEY.matcher(s);
        if (springMatcher.find()) {
            // "$1'$2$3'" -> "$1 ' protect($2) $3 '"
            s1 = springMatcher.group(1) + "'" + springMatcher.group(2).replace("'", "_") + springMatcher.group(3) + "'";
            return s1;
        }

        // Step 4: Handle nested quotes
        Matcher m = NESTED_QUOTE_PROTECTOR.matcher(s);
        if (m.find()) {
            String opening = m.group(2);
            String template = m.group(3);
            String closing = m.group(4);

            String escaped;
            if (opening.equals("\"")) {
                // Double-quoted: escape internal " as \"
                escaped = template.replace("\"", "\\\"");
            } else {
                // Single-quoted: escape internal ' as ''
                escaped = template.replace("'", "''");
            }
            s1 = m.group(1) + opening + escaped + closing;
            return s1;
        }
        return s;
    }

    protected ObjectMapper getMapper() {
        return defaultMapper;
    }

    private JsonFactory getJsonFactory() {
        return defaultYamlFactory;
    }

    protected Object nodeToValue(JsonNode node) {
        try {
            return getMapper().treeToValue(node, Object.class);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    //--- Fix for spring YAML configurations

    @SuppressWarnings({"rawtypes", "unchecked"})
    public void fixSpringYamlModel(Object obj) {
        if (obj instanceof List) {
            fixSpringYamlModels((List) obj);
        } else if (obj instanceof JsonArray) {
            fixSpringYamlModels((JsonArray) obj);
        } else if (obj instanceof Map) {
            fixSpringYamlModel(new JsonObject(((Map) obj)));
        } else if (obj instanceof JsonObject) {
            fixSpringYamlModel((JsonObject) obj);
        }
    }

    public void fixSpringYamlModels(List<?> list) {
        fixSpringYamlModel(new JsonArray(list));
    }

    public void fixSpringYamlModels(JsonArray list) {
        for (Object obj : list) {
            fixSpringYamlModel(obj);
        }
    }

    public void fixSpringYamlModel(JsonObject obj) {
        Set<String> keysToFix = null;
        for (Map.Entry<String, Object> e : obj) {
            String key = e.getKey();
            if (key.indexOf('.') >= 0) {
                if (keysToFix == null) {
                    keysToFix = new LinkedHashSet<>();
                }
                keysToFix.add(key);
            }
        }

        if (keysToFix != null) {
            for (String key : keysToFix) {
                Object value = obj.remove(key);

                JsonPointer ptr = JsonPointer.from("/" + key.replace('.', '/'));
                Object oldValue = ptr.queryJson(obj);
                // Note: both value and oldValue become wrapped: Map->JsonObject, List->JsonArray, etc...
                if (oldValue instanceof JsonObject && value instanceof JsonObject) {
                    // Merge new key-value pairs into old data
                    ((JsonObject) oldValue).getMap().putAll(((JsonObject) value).getMap());
                } else {
                    if (oldValue != null) {
                        LOG.warn("Overwriting key {}: old={}, new={}", key, oldValue, value);
                    }
                    ptr.writeJson(obj, value, true);
                }
            }
        }

        for (Map.Entry<String, Object> e : obj) {
            fixSpringYamlModel(e.getValue());
        }
    }

}
