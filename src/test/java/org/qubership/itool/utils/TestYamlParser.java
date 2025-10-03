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

    @Test
    public void testRealWorldHelmPatterns() {
        // Test cases based on real-world Helm chart patterns found in popular repositories

        // Basic .Values patterns
        assertEquals("  replicas: '{{ .Values.replicaCount }}'",
            normalizeLine("  replicas: {{ .Values.replicaCount }}"));
        assertEquals("  image: '{{ .Values.image.repository }}:"
                + "{{ .Values.image.tag | default .Chart.AppVersion }}'",
                normalizeLine("  image: {{ .Values.image.repository }}:"
                        + "{{ .Values.image.tag | default .Chart.AppVersion }}"));

        // .Chart patterns
        assertEquals("  name: '{{ .Chart.Name }}'",
            normalizeLine("  name: {{ .Chart.Name }}"));
        assertEquals("  version: '{{ .Chart.Version }}'",
            normalizeLine("  version: {{ .Chart.Version }}"));
        assertEquals("  appVersion: '{{ .Chart.AppVersion }}'",
            normalizeLine("  appVersion: {{ .Chart.AppVersion }}"));

        // .Release patterns
        assertEquals("  release: '{{ .Release.Name }}'",
            normalizeLine("  release: {{ .Release.Name }}"));
        assertEquals("  namespace: '{{ .Release.Namespace }}'",
            normalizeLine("  namespace: {{ .Release.Namespace }}"));
        assertEquals("  revision: '{{ .Release.Revision }}'",
            normalizeLine("  revision: {{ .Release.Revision }}"));

        // .Capabilities patterns
        assertEquals("  kubeVersion: '{{ .Capabilities.KubeVersion.GitVersion }}'",
            normalizeLine("  kubeVersion: {{ .Capabilities.KubeVersion.GitVersion }}"));
        assertEquals("  hasIngress: '{{ .Capabilities.APIVersions.Has \"networking.k8s.io/v1/Ingress\" }}'",
            normalizeLine("  hasIngress: {{ .Capabilities.APIVersions.Has \"networking.k8s.io/v1/Ingress\" }}"));
    }

    @Test
    public void testHelmTemplateFunctions() {
        // Test cases for common Helm template functions

        // String functions
        assertEquals("  name: '{{ .Values.name | default .Chart.Name | lower }}'",
            normalizeLine("  name: {{ .Values.name | default .Chart.Name | lower }}"));
        assertEquals("  hostname: '{{ .Values.hostname | upper }}'",
            normalizeLine("  hostname: {{ .Values.hostname | upper }}"));
        assertEquals("  prefix: '{{ .Values.prefix | trimSuffix \"-\" }}'",
            normalizeLine("  prefix: {{ .Values.prefix | trimSuffix \"-\" }}"));

        // Math functions
        assertEquals("  replicas: '{{ add .Values.replicas 1 }}'",
            normalizeLine("  replicas: {{ add .Values.replicas 1 }}"));
        assertEquals("  port: '{{ sub .Values.port 80 }}'",
            normalizeLine("  port: {{ sub .Values.port 80 }}"));

        // Logic functions
        assertEquals("  enabled: '{{ and .Values.enabled .Values.service.enabled }}'",
            normalizeLine("  enabled: {{ and .Values.enabled .Values.service.enabled }}"));
        assertEquals("  debug: '{{ or .Values.debug .Values.global.debug }}'",
            normalizeLine("  debug: {{ or .Values.debug .Values.global.debug }}"));
        assertEquals("  notEmpty: '{{ not (empty .Values.config) }}'",
            normalizeLine("  notEmpty: {{ not (empty .Values.config) }}"));

        // Conditional functions
        assertEquals(
                "  image: '{{ if .Values.image.tag }}{{ .Values.image.repository }}:{{ .Values.image.tag }}"
                + "{{ else }}{{ .Values.image.repository }}:latest{{ end }}'",
                normalizeLine(
                        "  image: {{ if .Values.image.tag }}{{ .Values.image.repository }}:{{ .Values.image.tag }}"
                        + "{{ else }}{{ .Values.image.repository }}:latest{{ end }}"));

        // Required function
        assertEquals("  database: '{{ required \"A valid database name is required\" .Values.database.name }}'",
            normalizeLine("  database: {{ required \"A valid database name is required\" .Values.database.name }}"));

        // Include function
        assertEquals("  config: '{{ include \"configmap.tpl\" . }}'",
            normalizeLine("  config: {{ include \"configmap.tpl\" . }}"));

        // Tpl function
        assertEquals("  annotation: '{{ tpl .Values.annotation . }}'",
            normalizeLine("  annotation: {{ tpl .Values.annotation . }}"));
    }

    @Test
    public void testHelmControlStructures() {
        // Test cases for Helm control structures (these should be rejected by acceptLine)

        // if statements
        assertFalse(acceptLine("{{ if .Values.enabled }}"));
        assertFalse(acceptLine("{{- if .Values.enabled }}"));
        assertFalse(acceptLine("{{ if and .Values.enabled .Values.service.enabled }}"));
        assertFalse(acceptLine("{{ else }}"));
        assertFalse(acceptLine("{{ end }}"));
        assertFalse(acceptLine("{{- end }}"));

        // range statements
        assertFalse(acceptLine("{{ range .Values.volumes }}"));
        assertFalse(acceptLine("{{- range .Values.volumes }}"));
        assertFalse(acceptLine("{{ range $key, $value := .Values.config }}"));
        assertFalse(acceptLine("{{ end }}"));

        // with statements
        assertFalse(acceptLine("{{ with .Values.service }}"));
        assertFalse(acceptLine("{{- with .Values.service }}"));
        assertFalse(acceptLine("{{ end }}"));

        // define statements
        assertFalse(acceptLine("{{ define \"mychart.labels\" }}"));
        assertFalse(acceptLine("{{- define \"mychart.labels\" }}"));
        assertFalse(acceptLine("{{ end }}"));

        // template calls
        assertFalse(acceptLine("{{ template \"mychart.labels\" . }}"));
        assertFalse(acceptLine("{{- template \"mychart.labels\" . }}"));
    }

    @Test
    public void testInlineControlStructures() {
        // Test cases for inline control structures (these should be accepted and normalized)

        // Inline if statements
        assertEquals("  name: '{{ if .Values.name }}{{ .Values.name }}{{ else }}default{{ end }}'",
            normalizeLine("  name: {{ if .Values.name }}{{ .Values.name }}{{ else }}default{{ end }}"));
        assertEquals(
                "  image: '{{ if .Values.image.tag }}{{ .Values.image.repository }}:{{ .Values.image.tag }}"
                + "{{ else }}{{ .Values.image.repository }}:latest{{ end }}'",
                normalizeLine(
                        "  image: {{ if .Values.image.tag }}{{ .Values.image.repository }}:{{ .Values.image.tag }}"
                        + "{{ else }}{{ .Values.image.repository }}:latest{{ end }}"));
        assertEquals("  port: '{{ if .Values.service.port }}{{ .Values.service.port }}{{ else }}80{{ end }}'",
            normalizeLine("  port: {{ if .Values.service.port }}{{ .Values.service.port }}{{ else }}80{{ end }}"));

        // Inline with statements
        assertEquals("  host: '{{ with .Values.ingress }}{{ .host }}{{ end }}'",
            normalizeLine("  host: {{ with .Values.ingress }}{{ .host }}{{ end }}"));
        assertEquals("  tls: '{{ with .Values.ingress.tls }}{{ .enabled }}{{ end }}'",
            normalizeLine("  tls: {{ with .Values.ingress.tls }}{{ .enabled }}{{ end }}"));

        // Inline range statements (less common but possible)
        assertEquals("  volumes: '{{ range .Values.volumes }}{{ .name }},{{ end }}'",
            normalizeLine("  volumes: {{ range .Values.volumes }}{{ .name }},{{ end }}"));
        assertEquals("  labels: '{{ range $key, $value := .Values.labels }}{{ $key }}={{ $value }},{{ end }}'",
            normalizeLine("  labels: {{ range $key, $value := .Values.labels }}{{ $key }}={{ $value }},{{ end }}"));

        // Complex inline conditionals
        assertEquals(
                "  replicas: '{{ if .Values.autoscaling.enabled }}{{ .Values.autoscaling.minReplicas }}"
                + "{{ else }}{{ .Values.replicaCount }}{{ end }}'",
                normalizeLine(
                        "  replicas: {{ if .Values.autoscaling.enabled }}{{ .Values.autoscaling.minReplicas }}"
                        + "{{ else }}{{ .Values.replicaCount }}{{ end }}"));
        assertEquals("  strategy: '{{ if .Values.strategy }}{{ .Values.strategy }}{{ else }}RollingUpdate{{ end }}'",
                normalizeLine(
                        "  strategy: {{ if .Values.strategy }}{{ .Values.strategy }}{{ else }}RollingUpdate{{ end }}"));

        // Inline conditionals with whitespace control
        assertEquals("  name: '{{- if .Values.name }}{{ .Values.name }}{{ else }}default{{ end }}'",
            normalizeLine("  name: {{- if .Values.name }}{{ .Values.name }}{{ else }}default{{ end }}"));
        assertEquals(
                "  image: '{{ if .Values.image.tag }}{{ .Values.image.repository }}:"
                + "{{ .Values.image.tag }}{{ else }}{{ .Values.image.repository }}:latest{{ end -}}'",
                normalizeLine(
                        "  image: {{ if .Values.image.tag }}{{ .Values.image.repository }}:"
                        + "{{ .Values.image.tag }}{{ else }}{{ .Values.image.repository }}:latest{{ end -}}"));

        // Nested inline conditionals
        assertEquals(
                "  value: '{{ if .Values.config }}{{ if .Values.config.debug }}debug{{ else }}"
                + "production{{ end }}{{ else }}default{{ end }}'",
                normalizeLine(
                        "  value: {{ if .Values.config }}{{ if .Values.config.debug }}debug{{ else }}"
                        + "production{{ end }}{{ else }}default{{ end }}"));

        // Inline conditionals in sequence items
        assertEquals(
                "  - '{{ if .Values.image.tag }}{{ .Values.image.repository }}:{{ .Values.image.tag }}"
                + "{{ else }}{{ .Values.image.repository }}:latest{{ end }}'",
                normalizeLine(
                        "  - {{ if .Values.image.tag }}{{ .Values.image.repository }}:{{ .Values.image.tag }}"
                        + "{{ else }}{{ .Values.image.repository }}:latest{{ end }}"));
        assertEquals("  - '{{ with .Values.service }}{{ .name }}{{ end }}'",
            normalizeLine("  - {{ with .Values.service }}{{ .name }}{{ end }}"));
    }

    @Test
    public void testHelmEdgeCases() {
        // Test cases for edge cases and special syntax

        // Whitespace control
        assertEquals("  value: '{{- .Values.value -}}'",
            normalizeLine("  value: {{- .Values.value -}}"));
        assertEquals("  value: '{{+ .Values.value +}}'",
            normalizeLine("  value: {{+ .Values.value +}}"));

        // Comments
        assertEquals("  # value: '{{ .Values.value }}'",
            normalizeLine("  # value: {{ .Values.value }}"));
        assertEquals("  value: '{{/* This is a comment */ .Values.value }}'",
            normalizeLine("  value: {{/* This is a comment */ .Values.value }}"));

        // Nested quotes
        assertEquals("  message: '{{ printf \"Hello %s\" .Values.name }}'",
            normalizeLine("  message: {{ printf \"Hello %s\" .Values.name }}"));
        assertEquals("  message: '{{ printf \"Hello %s\" .Values.name | quote }}'",
            normalizeLine("  message: {{ printf \"Hello %s\" .Values.name | quote }}"));

        // Complex expressions
        assertEquals("  value: '{{ .Values.config | toYaml | indent 2 }}'",
            normalizeLine("  value: {{ .Values.config | toYaml | indent 2 }}"));
        assertEquals("  json: '{{ .Values.data | toJson }}'",
            normalizeLine("  json: {{ .Values.data | toJson }}"));

        // Pipeline with multiple functions
        assertEquals("  name: '{{ .Values.name | default .Chart.Name | lower | trunc 63 | trimSuffix \"-\" }}'",
            normalizeLine("  name: {{ .Values.name | default .Chart.Name | lower | trunc 63 | trimSuffix \"-\" }}"));

        // Variables
        assertEquals("  value: '{{ $name := .Values.name }}{{ $name }}'",
            normalizeLine("  value: {{ $name := .Values.name }}{{ $name }}"));
        assertEquals("  value: '{{- $name := .Values.name -}}{{$name}}'",
            normalizeLine("  value: {{- $name := .Values.name -}}{{$name}}"));
    }

    @Test
    public void testHelmArrayAndObjectPatterns() {
        // Test cases for array and object patterns

        // Array values
        assertEquals("  - '{{ .Values.image.repository }}'",
            normalizeLine("  - {{ .Values.image.repository }}"));
        assertEquals("  - '{{ .Values.image.repository }}:{{ .Values.image.tag }}'",
            normalizeLine("  - {{ .Values.image.repository }}:{{ .Values.image.tag }}"));

        // Object values
        assertEquals("  image: '{{ .Values.image.repository }}:{{ .Values.image.tag }}'",
            normalizeLine("  image: {{ .Values.image.repository }}:{{ .Values.image.tag }}"));
        assertEquals("  labels: '{{ include \"mychart.labels\" . }}'",
            normalizeLine("  labels: {{ include \"mychart.labels\" . }}"));

        // Complex nested values
        assertEquals("  env: '{{ .Values.env | toYaml }}'",
            normalizeLine("  env: {{ .Values.env | toYaml }}"));
        assertEquals("  data: '{{ .Values.configMap | toYaml }}'",
            normalizeLine("  data: {{ .Values.configMap | toYaml }}"));

        // Port patterns
        assertEquals("  port: '{{ .Values.service.port | default 80 }}'",
            normalizeLine("  port: {{ .Values.service.port | default 80 }}"));
        assertEquals("  targetPort: '{{ .Values.service.targetPort | default .Values.service.port }}'",
            normalizeLine("  targetPort: {{ .Values.service.targetPort | default .Values.service.port }}"));
    }

    @Test
    public void testHelmSecurityAndValidation() {
        // Test cases for security and validation patterns

        // Required values
        assertEquals("  name: '{{ required \"A valid name is required\" .Values.name }}'",
            normalizeLine("  name: {{ required \"A valid name is required\" .Values.name }}"));
        assertEquals("  database: '{{ required \"Database name is required\" .Values.database.name }}'",
            normalizeLine("  database: {{ required \"Database name is required\" .Values.database.name }}"));

        // Fail function
        assertEquals("  value: '{{ fail \"This deployment is not allowed\" }}'",
            normalizeLine("  value: {{ fail \"This deployment is not allowed\" }}"));

        // Validation patterns
        assertEquals("  replicas: '{{ .Values.replicas | int | max 10 | min 1 }}'",
            normalizeLine("  replicas: {{ .Values.replicas | int | max 10 | min 1 }}"));
        assertEquals("  port: '{{ .Values.port | int | max 65535 | min 1 }}'",
            normalizeLine("  port: {{ .Values.port | int | max 65535 | min 1 }}"));

        // Security contexts
        assertEquals("  runAsUser: '{{ .Values.securityContext.runAsUser | default 1000 }}'",
            normalizeLine("  runAsUser: {{ .Values.securityContext.runAsUser | default 1000 }}"));
        assertEquals("  runAsNonRoot: '{{ .Values.securityContext.runAsNonRoot | default true }}'",
            normalizeLine("  runAsNonRoot: {{ .Values.securityContext.runAsNonRoot | default true }}"));
    }

    @Test
    public void testHelmResourcePatterns() {
        // Test cases for common Kubernetes resource patterns

        // Service patterns
        assertEquals("  type: '{{ .Values.service.type | default \"ClusterIP\" }}'",
            normalizeLine("  type: {{ .Values.service.type | default \"ClusterIP\" }}"));
        assertEquals("  clusterIP: '{{ .Values.service.clusterIP | default \"\" }}'",
            normalizeLine("  clusterIP: {{ .Values.service.clusterIP | default \"\" }}"));

        // Ingress patterns
        assertEquals("  host: '{{ .Values.ingress.host | default \"\" }}'",
            normalizeLine("  host: {{ .Values.ingress.host | default \"\" }}"));
        assertEquals("  tls: '{{ .Values.ingress.tls | toYaml }}'",
            normalizeLine("  tls: {{ .Values.ingress.tls | toYaml }}"));

        // ConfigMap patterns
        assertEquals("  data: '{{ .Values.configMap.data | toYaml }}'",
            normalizeLine("  data: {{ .Values.configMap.data | toYaml }}"));
        assertEquals("  binaryData: '{{ .Values.configMap.binaryData | toYaml }}'",
            normalizeLine("  binaryData: {{ .Values.configMap.binaryData | toYaml }}"));

        // Secret patterns
        assertEquals("  data: '{{ .Values.secret.data | toYaml }}'",
            normalizeLine("  data: {{ .Values.secret.data | toYaml }}"));
        assertEquals("  type: '{{ .Values.secret.type | default \"Opaque\" }}'",
            normalizeLine("  type: {{ .Values.secret.type | default \"Opaque\" }}"));
    }
}
