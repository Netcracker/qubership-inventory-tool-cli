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
import org.qubership.itool.modules.graph.Graph;

import java.time.Instant;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Helper for creating and attaching entries to the {@code processingIssues} array of a graph vertex.
 * Callers are responsible for choosing a category identifier that aligns with the registry they use.
 */
public final class ProcessingIssueBuilder {

    private static final String FIELD_CATEGORY = "category";
    private static final String FIELD_SUMMARY = "summary";
    private static final String FIELD_SOURCE = "source";
    private static final String FIELD_AFFECTED = "affected";
    private static final String FIELD_TIMESTAMP = "timestamp";
    private static final String FIELD_DETAILS = "details";

    private final JsonObject issue;
    private JsonObject details;

    public ProcessingIssueBuilder(String category, String summary) {
        this.issue = new JsonObject();
        this.issue.put(FIELD_CATEGORY, requireNonBlank("category", category));
        this.issue.put(FIELD_SUMMARY, requireNonBlank("summary", summary));
    }

    /**
     * Sets the {@code source} object, cloning the supplied JSON to avoid caller-side mutation.
     * Passing {@code null} or an empty JSON removes the field.
     *
     * @param source producer metadata (task, stage, etc.)
     * @return this builder
     */
    public ProcessingIssueBuilder source(JsonObject source) {
        if (source == null || source.isEmpty()) {
            issue.remove(FIELD_SOURCE);
            return this;
        }
        issue.put(FIELD_SOURCE, source.copy());
        return this;
    }

    /**
     * Convenience overload for setting source fields using task/stage strings.
     * Omits empty values and removes the field if both arguments are blank.
     *
     * @param task pipeline task that raised the issue
     * @param stage optional processing stage name
     * @return this builder
     */
    public ProcessingIssueBuilder source(String task, String stage) {
        JsonObject source = new JsonObject();
        if (task != null && !task.isBlank()) {
            source.put("task", task);
        }
        if (stage != null && !stage.isBlank()) {
            source.put("stage", stage);
        }
        return source.isEmpty() ? source(null) : source(source);
    }

    /**
     * Sets the {@code affected} object indicating which sub-entity is impacted.
     * Passing {@code null} or an empty JSON removes the field.
     *
     * @param affected descriptor of impacted entity (type/id, etc.)
     * @return this builder
     */
    public ProcessingIssueBuilder affected(JsonObject affected) {
        if (affected == null || affected.isEmpty()) {
            issue.remove(FIELD_AFFECTED);
            return this;
        }
        issue.put(FIELD_AFFECTED, affected.copy());
        return this;
    }

    /**
     * Convenience overload for setting affected entity attributes.
     *
     * @param type type of the impacted entity
     * @param id identifier of the impacted entity
     * @return this builder
     */
    public ProcessingIssueBuilder affected(String type, String id) {
        JsonObject affected = new JsonObject();
        if (type != null && !type.isBlank()) {
            affected.put("type", type);
        }
        if (id != null && !id.isBlank()) {
            affected.put("id", id);
        }
        return affected.isEmpty() ? affected((JsonObject) null) : affected(affected);
    }

    /**
     * Sets an ISO timestamp based on the supplied {@link Instant}.
     * Passing {@code null} removes the timestamp.
     *
     * @param timestamp moment when the issue was detected
     * @return this builder
     */
    public ProcessingIssueBuilder timestamp(Instant timestamp) {
        if (timestamp == null) {
            issue.remove(FIELD_TIMESTAMP);
        } else {
            issue.put(FIELD_TIMESTAMP, timestamp.toString());
        }
        return this;
    }

    /**
     * Sets the timestamp directly from a string. Expects ISO-8601 formatting.
     * Passing {@code null} or blank removes the timestamp.
     *
     * @param isoTimestamp timestamp representation
     * @return this builder
     */
    public ProcessingIssueBuilder timestamp(String isoTimestamp) {
        if (isoTimestamp == null || isoTimestamp.isBlank()) {
            issue.remove(FIELD_TIMESTAMP);
        } else {
            issue.put(FIELD_TIMESTAMP, isoTimestamp);
        }
        return this;
    }

    /**
     * Adds or removes a single {@code details} entry. Null values remove the key and clean up the
     * entire object if it becomes empty.
     *
     * @param key detail key to mutate
     * @param value value to store, or {@code null} to remove
     * @return this builder
     */
    public ProcessingIssueBuilder detail(String key, Object value) {
        Objects.requireNonNull(key, "key");
        if (value == null) {
            if (details != null) {
                details.remove(key);
                if (details.isEmpty()) {
                    details = null;
                }
            }
            return this;
        }
        ensureDetails().put(key, value);
        return this;
    }

    /**
     * Replaces the {@code details} object wholesale, cloning the value to avoid caller mutation.
     * Passing {@code null} or empty JSON removes the field.
     *
     * @param details detail payload
     * @return this builder
     */
    public ProcessingIssueBuilder details(JsonObject details) {
        this.details = (details == null || details.isEmpty()) ? null : details.copy();
        return this;
    }

    /**
     * Populates or mutates the {@code details} object via the supplied lambda.
     * Useful when several fields need to be set at once without building a temporary {@link JsonObject}.
     * If the mutator leaves the object empty, the {@code details} field is removed from the issue.
     */
    public ProcessingIssueBuilder details(Consumer<JsonObject> mutator) {
        Objects.requireNonNull(mutator, "mutator");
        mutator.accept(ensureDetails());
        if (details != null && details.isEmpty()) {
            details = null;
        }
        return this;
    }

    /**
     * Builds a defensive copy of the current issue payload without attaching it to a vertex.
     *
     * @return immutable view of the issue JSON
     */
    public JsonObject build() {
        JsonObject built = issue.copy();
        if (details != null && !details.isEmpty()) {
            built.put(FIELD_DETAILS, details.copy());
        } else {
            built.remove(FIELD_DETAILS);
        }
        return built;
    }

    /**
     * Builds the issue and appends it to the vertex's {@code processingIssues} array.
     * If the array does not exist, it is created.
     *
     * @param vertex vertex JSON to mutate
     * @return built issue instance stored in the array
     */
    public JsonObject addTo(JsonObject vertex) {
        Objects.requireNonNull(vertex, "vertex");
        JsonArray issues = ensureIssuesArray(vertex);
        JsonObject built = build();
        issues.add(built);
        return built;
    }

    /**
     * Appends an already built processing issue to the vertex's {@code processingIssues} array.
     * The supplied issue is copied before being stored to avoid external mutation.
     *
     * @param vertex vertex JSON to mutate
     * @param issue existing issue payload to append
     * @return stored copy of the supplied issue
     */
    public static JsonObject addIssue(JsonObject vertex, JsonObject issue) {
        Objects.requireNonNull(vertex, "vertex");
        Objects.requireNonNull(issue, "issue");
        JsonArray issues = ensureIssuesArray(vertex);
        JsonObject stored = issue.copy();
        issues.add(stored);
        return stored;
    }

    /**
     * Ensures that the given vertex has a {@code processingIssues} array and returns it.
     *
     * @param vertex vertex JSON to inspect/mutate
     * @return existing or newly created {@link JsonArray} of processing issues
     */
    public static JsonArray ensureIssuesArray(JsonObject vertex) {
        JsonArray issues = vertex.getJsonArray(Graph.F_PROCESSING_ISSUES);
        if (issues == null) {
            issues = new JsonArray();
            vertex.put(Graph.F_PROCESSING_ISSUES, issues);
        }
        return issues;
    }

    private JsonObject ensureDetails() {
        if (details == null) {
            details = new JsonObject();
        }
        return details;
    }

    private static String requireNonBlank(String fieldName, String value) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
