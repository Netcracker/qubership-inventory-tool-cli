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

package org.qubership.itool.modules.gremlin2.step.map.flat;

import org.qubership.itool.modules.graph.BasicGraph;
import org.qubership.itool.modules.gremlin2.DefaultTraverser;
import org.qubership.itool.modules.gremlin2.Traversal;
import org.qubership.itool.modules.gremlin2.Traverser;
import org.qubership.itool.modules.gremlin2.step.AbstractStep;
import org.qubership.itool.modules.gremlin2.step.ByModulating;

import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class GlobStep extends FlatMapStep<JsonObject, JsonObject> implements ByModulating {

    private static final String BY_DEFAULT = "name";
    public static final String GLOB_DELIMETER = "/";

    private String pattern;
    private List<Object> patternList;   // Contains strings and precompiled Patterns
    private String byProperty;
    private Traversal<?, ?> byTraversal;

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + pattern + ")";
    }

    @SuppressWarnings("rawtypes")
    public GlobStep(Traversal.Admin traversal, String pattern) {
        super(traversal);
        this.pattern = pattern;

        String[] rawPatternArray = this.pattern.split(GLOB_DELIMETER);
        patternList = new ArrayList<>(rawPatternArray.length);
        for (String patternElement: rawPatternArray) {
            if (patternElement.isEmpty()) {
                // Ignore initial slash, trailing slash, double slash
            } else if ("**".equals(patternElement)) {
                if (patternList.size() == 0 || ! "**".equals(patternList.get(patternList.size() - 1))) {
                    // Squash repetitive "**"-s
                    // For even crazier cases (e.g.: "**/*" vs "*/**") use dedup() if needed
                    patternList.add(patternElement);
                }
            } else if (patternElement.contains("*") || patternElement.contains("?")) {
                String regex = "^" + patternElement
                    .replaceAll("\\.", "\\\\.")
                    .replaceAll("\\*", "\\.*")
                    .replaceAll("\\?", "\\.")
                    + "$";
                Pattern regexPattern = Pattern.compile(regex);
                patternList.add(regexPattern);
            } else {
                patternList.add(patternElement);
            }
        }
    }

    @Override
    protected List<JsonObject> flatMap(Traverser.Admin<JsonObject> traverser) {
        List<JsonObject> result = new ArrayList<>();
        JsonObject sourceVertex = requireSourceVertex(traverser);

        patternMatching(sourceVertex, result, patternList, true);

        return result;
    }

    private void patternMatching(JsonObject source, List<JsonObject> result, List<Object> patterns, boolean letDoubleWildcard) {

        if (patterns.isEmpty()) {   // Nothing matches empty pattern
            return;
        }

        String by = (byProperty == null) ? BY_DEFAULT : byProperty;
        Object value = source.getString(by);
        if (value == null) {
            return;
        }

        Object patternElement = patterns.get(0);
        List<Object> patternsTail = patterns.subList(1, patterns.size());

        if ("**".equals(patternElement)) {
            if (letDoubleWildcard) {
                addVertexIfLast(source, result, patterns);    // "**" -> add this
                patternMatching(source, result, patternsTail, true);    // "**/a/b" -> check this against "a/b"
            }
            List<JsonObject> successors = fetchSuccessors(source);
            for (JsonObject successor: successors) {
                // 1) "**/a/b" -> check successors against "**/a/b", not allowing them to match
                // themselves against starting "**", but still allowing to match their successors against **.
                // Otherwise, successors are matched against "a/b" twice and may duplicate results.
                // 2) "**" -> check successors against "**", allowing them to match themselves against "**"
                patternMatching(successor, result, patterns, patterns.size() == 1);
                // "**/a/b" -> check successors against "a/b"
                patternMatching(successor, result, patternsTail, true);
            }
        } else if (patternElement instanceof Pattern) {
            Pattern regexPattern = (Pattern) patternElement;
            if (regexPattern.matcher(value.toString()).matches()) {
                addVertexIfLast(source, result, patterns);          // "*" -> add this
                List<JsonObject> successors = fetchSuccessors(source);
                for (JsonObject successor: successors) {
                    patternMatching(successor, result, patternsTail, true); // "*/a/b" -> check successors against "a/b"
                }
            }
        } else if (patternElement.equals(value)) {
            addVertexIfLast(source, result, patterns);          // this matches "a" -> add this
            List<JsonObject> successors = fetchSuccessors(source);
            for (JsonObject successor: successors) {
                patternMatching(successor, result, patternsTail, true); // "a/b/c", this matches "a" -> check successors against "b/c"
            }
        }
    }

    private void addVertexIfLast(JsonObject sourceVertex, List<JsonObject> result, List<Object> patterns) {
        if (patterns.size() == 1) {
            result.add(sourceVertex);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private List<JsonObject> fetchSuccessors(JsonObject sourceVertex) {
        if (this.byTraversal != null) {
            List<Traverser.Admin<JsonObject>> trList = new ArrayList<>();
            trList.add(new DefaultTraverser<>(sourceVertex, sourceVertex));
            Traversal.Admin cloneTraversal = prepareInnerTraversal((Traversal.Admin)this.byTraversal, trList);
            return cloneTraversal.toList();
        }
        BasicGraph graph = this.traversal.getGraph();
        return graph.getSuccessors(sourceVertex.getString("id"), true);
    }

    @Override
    public void modulateBy(String byProperty) throws UnsupportedOperationException {
        this.byProperty = byProperty;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void modulateBy(Traversal traversal) throws UnsupportedOperationException {
        this.byTraversal = traversal;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void clear() {
        clearTraversal((Traversal.Admin)this.byTraversal);
    }

    @Override
    public AbstractStep<JsonObject, JsonObject> clone() {
        GlobStep clone = (GlobStep)super.clone();
        clone.pattern = this.pattern;
        clone.byProperty = this.byProperty;
        clone.byTraversal = this.byTraversal;
        return clone;
    }

}
