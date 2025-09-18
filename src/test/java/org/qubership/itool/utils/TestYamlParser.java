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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestYamlParser {


    YamlParser parser;

    @BeforeAll
    public void init() {
        parser = new YamlParser();
    }

    @Test
    public void multiSection() throws IOException {
        String yaml =
            "---\n" +
                "a: b\n" +
                "\n" +
                "---\n" +
                "- c\n" +
                "- d\n";
        List<Object> data = parser.parseYamlData(yaml, "<generated>");
        assertEquals(2, data.size());
        assertTrue(data.get(0) instanceof Map);
        assertTrue(data.get(1) instanceof List);
    }

    @Test
    public void testAcept() {
        assertTrue(acceptLine(""));
        assertTrue(acceptLine("---"));
        assertTrue(acceptLine("kind: Deployment"));
        assertTrue(acceptLine("            - censored: censored"));
        assertTrue(acceptLine("              censored: '{{ .Values.SOME_VALUE }}'"));
        assertTrue(acceptLine("            - "));
        assertTrue(acceptLine("            - '{{ .Values.SOME_VALUE }}'"));
        assertTrue(acceptLine("            - -jar"));
        assertTrue(acceptLine("  censored.version: @censored.censored.version@"));

        assertFalse(acceptLine("{{ if eq .Values.SOME_VALUE \"KUBERNETES\" }}"));
        assertFalse(acceptLine("{ { if eq .Values.SOME_VALUE \"KUBERNETES\" } }"));
        assertFalse(acceptLine("{{ end }}"));
        assertFalse(acceptLine("  {{ end }}"));
        assertFalse(acceptLine("  { { end } }"));
        assertFalse(acceptLine("{{- if eq .Values.SOME_VALUE true }}"));
        assertFalse(acceptLine("  { {- if eq .Values.SOME_VALUE true }}"));
    }

    @Test
    public void testNormalization() {

        assertEquals("    censored/component: censored",
            normalizeLine("    censored/component: censored"));

        assertEquals("            censored1: '{{ .Values.SOME_VALUE }}'",
            normalizeLine("            censored1: {{ .Values.SOME_VALUE }}"));
        assertEquals("            censored2: '{{ .Values.SOME_VALUE }}'",
            normalizeLine("            censored2: {{ .Values.SOME_VALUE }}  \t   "));
        assertEquals("            censored3: '{{ .Values.SOME_VALUE }}'    ",
            normalizeLine("            censored3: '{{ .Values.SOME_VALUE }}'    "));
        assertEquals("            censored4: \"{{ .Values.SOME_VALUE }}\"   ",
            normalizeLine("            censored4: \"{{ .Values.SOME_VALUE }}\"   "));

        assertEquals("  replicas1: '{ { .Values.SOME_VALUE } }'",
            normalizeLine("  replicas1: { { .Values.SOME_VALUE } }  "));
        assertEquals("  replicas2: '{ { .Values.SOME_VALUE } }'",
            normalizeLine("  replicas2: '{ { .Values.SOME_VALUE } }'"));
        assertEquals("  replicas3: \"{ { .Values.SOME_VALUE } }\"",
            normalizeLine("  replicas3: \"{ { .Values.SOME_VALUE } }\""));

        assertEquals("                    censored: '{{ .Values.SOME_VALUE }}-censored-censored'",
            normalizeLine("                    censored: '{{ .Values.SOME_VALUE }}-censored-censored'"));

        assertEquals("                censored: '{{ .Values.SOME_VALUE }}:{{ .Values.SOME_OTHER_VALUE }}'",
            normalizeLine("                censored: '{{ .Values.SOME_VALUE }}:{{ .Values.SOME_OTHER_VALUE }}'"));

        assertEquals("    censored: '{{ .Values.SOME_VALUE }}-censored'",
            normalizeLine("    censored: {{ .Values.SOME_VALUE }}-censored"));
        assertEquals("    censored: '{{ .Values.SOME_VALUE }}-censored'",
            normalizeLine("    censored: {{ .Values.SOME_VALUE }}-censored   "));
        assertEquals("    censored: \"{{ .Values.SOME_VALUE }}-censored\"",
            normalizeLine("    censored: \"{{ .Values.SOME_VALUE }}-censored\""));

        assertEquals("    censored.censored.io/censored: '{{ .Values.SOME_VALUE }}-censored'",
            normalizeLine("    censored.censored.io/censored: {{ .Values.SOME_VALUE }}-censored"));
        assertEquals("    censored.censored.io/censored: '{{ .Values.SOME_VALUE }}-censored'",
            normalizeLine("    censored.censored.io/censored: '{{ .Values.SOME_VALUE }}-censored'"));

        assertEquals("    - censored",
            normalizeLine("    - censored"));

        assertEquals("      - '{{ .Values.SOME_VALUE }}'",
            normalizeLine("      - {{ .Values.SOME_VALUE }}"));
        assertEquals("      - '{{ .Values.SOME_VALUE }}'",
            normalizeLine("      - {{ .Values.SOME_VALUE }}   "));
        assertEquals("      - '{{ .Values.SOME_VALUE }}'",
            normalizeLine("      - '{{ .Values.SOME_VALUE }}'"));

        assertEquals("  censored.version: '@censored-censored-censored.censored.censored.version@'",
            normalizeLine("  censored.version: @censored-censored-censored.censored.censored.version@"));
        assertEquals("  censored.version: '@censored-censored-censored.censored.censored.version@'",
            normalizeLine("  censored.version: '@censored-censored-censored.censored.censored.version@'"));

        assertEquals("  censored: '${ SOME_VALUE ? SOME_VALUE : 3 }'",
            normalizeLine("  censored: ${ SOME_VALUE ? SOME_VALUE : 3 }"));
        assertEquals("  censored: '${ SOME_VALUE ? SOME_VALUE : __}'",
            normalizeLine("  censored: ${ SOME_VALUE ? SOME_VALUE : ''}"));


        assertEquals("'{{- $censored := $.Values.SOME_VALUES -}}'",
            normalizeLine("{{- $censored := $.Values.SOME_VALUES -}}"));
        assertEquals("'{{ censored \"test-value.value\" . | value 4 | value }}'",
            normalizeLine("{{ censored \"test-value.value\" . | value 4 | value }}"));
        assertEquals("'{{- $isCensored := or (eq $.Values.SOME_VALUE true) (gt $censored 1)  -}}'",
            normalizeLine("{{- $isCensored := or (eq $.Values.SOME_VALUE true) (gt $censored 1)  -}}"));
        assertEquals("'{{ filter \"some-value.value.data\" . | action 4 | action }}'",
            normalizeLine("{{ filter \"some-value.value.data\" . | action 4 | action }}"));

    }

    private boolean acceptLine(String s) {
        return parser.acceptLine(s);
    }

    private String normalizeLine(String s) {
        return parser.normalizeLine(s);
    }

    @Test
    public void testSpringNormalization() throws IOException {
        try (Reader reader = new InputStreamReader(
            FSUtils.openRawUrlStream(getClass(), "classpath:/yaml-parser/spring.yml"), JsonUtils.UTF_8)) {
            List<Object> data = parser.parseYaml(reader, "yaml-parser/spring.yml");
            parser.fixSpringYamlModel(data);

            JsonArray expected = JsonUtils.readJsonResource(getClass(),
                "classpath:/yaml-parser/spring-canonical.json", JsonArray.class);
            assertEquals(expected, new JsonArray(data));
        }
    }
}
