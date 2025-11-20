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
import org.junit.jupiter.api.Test;
import org.qubership.itool.modules.graph.Graph;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.qubership.itool.utils.ProcessingIssueCategories.PARSING;
import static org.qubership.itool.utils.ProcessingIssueCategories.PROCESSING;

class ProcessingIssueBuilderTest {

    @Test
    void buildMinimalIssue() {
        JsonObject issue = new ProcessingIssueBuilder(PARSING, "Missing message").build();
        assertEquals(PARSING, issue.getString("category"));
        assertEquals("Missing message", issue.getString("summary"));
        assertFalse(issue.containsKey("details"));
        assertFalse(issue.containsKey("source"));
    }

    @Test
    void addSourceFromStrings() {
        JsonObject issue = new ProcessingIssueBuilder(PARSING, "Invalid yaml")
                .source("ParseYamlFilesVerticle", "")
                .build();

        JsonObject source = issue.getJsonObject("source");
        assertNotNull(source);
        assertEquals(1, source.size());
        assertEquals("ParseYamlFilesVerticle", source.getString("task"));
    }

    @Test
    void detailCreatesDetailsObject() {
        JsonObject issue = new ProcessingIssueBuilder(PROCESSING, "Invalid data")
                .detail("field", "messageText")
                .build();

        JsonObject details = issue.getJsonObject("details");
        assertNotNull(details);
        assertEquals("messageText", details.getString("field"));
    }

    @Test
    void detailAddsAndRemovesEntries() {
        JsonObject issue = new ProcessingIssueBuilder(PROCESSING, "Invalid data")
                .detail("field", "messageText")
                .detail("field", null)
                .build();

        assertFalse(issue.containsKey("details"));
    }

    @Test
    void detailsMutatorPopulatesMultipleFields() {
        JsonObject issue = new ProcessingIssueBuilder(PROCESSING, "Failed to parse file")
                .details(details -> {
                    details.put("file", "errorCodes.md");
                    details.put("blocking", true);
                })
                .build();

        JsonObject details = issue.getJsonObject("details");
        assertNotNull(details);
        assertEquals("errorCodes.md", details.getString("file"));
        assertTrue(details.getBoolean("blocking"));
    }

    @Test
    void addToCreatesArrayAndReturnsStoredIssue() {
        JsonObject vertex = new JsonObject();
        JsonObject stored = new ProcessingIssueBuilder(PROCESSING, "Failed to parse file")
                .timestamp(Instant.parse("2024-01-02T03:04:05Z"))
                .addTo(vertex);

        JsonArray issues = vertex.getJsonArray(Graph.F_PROCESSING_ISSUES);
        assertNotNull(issues);
        assertEquals(1, issues.size());
        assertEquals(stored, issues.getJsonObject(0));
        assertEquals("2024-01-02T03:04:05Z", stored.getString("timestamp"));
    }

    @Test
    void addIssueAppendsExistingIssue() {
        JsonObject vertex = new JsonObject();
        JsonObject issue = new ProcessingIssueBuilder(PROCESSING, "Existing issue").build();

        JsonObject stored = ProcessingIssueBuilder.addIssue(vertex, issue);

        JsonArray issues = vertex.getJsonArray(Graph.F_PROCESSING_ISSUES);
        assertNotNull(issues);
        assertEquals(1, issues.size());
        assertEquals(stored, issues.getJsonObject(0));
        assertEquals("Existing issue", stored.getString("summary"));
        assertTrue(stored != issue);
    }

    @Test
    void rejectsBlankInputs() {
        assertThrows(IllegalArgumentException.class, () -> new ProcessingIssueBuilder(PARSING, " "));
        assertThrows(NullPointerException.class, () -> new ProcessingIssueBuilder(null, "summary"));
    }
}
