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

package org.qubership.itool.tasks.dependency;

import org.qubership.itool.tasks.AbstractAggregationTaskVerticle;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.WorkerExecutor;
import io.vertx.core.impl.cpu.CpuCoreSensor;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import org.apache.commons.lang3.StringUtils;
import org.qubership.itool.modules.graph.Graph;
import org.qubership.itool.utils.JsonUtils;
import org.qubership.itool.utils.FutureUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.qubership.itool.modules.graph.Graph.F_DIRECTORY;
import static org.qubership.itool.modules.graph.Graph.F_ID;

/**
 * Parse maven dependencies stored to "${component.directoryPath}/target/dependency_tree.json} by CI pipeline.
 * or to "output/dependencies/${component.id}_dependency.txt" by {@link MavenDependencyDumpExtractVerticle}.
 */
public class MavenDependencyDumpParseVerticle extends AbstractAggregationTaskVerticle {
    protected Logger LOG = LoggerFactory.getLogger(MavenDependencyDumpParseVerticle.class);
    // Have to be global to avoid closing by GC
    private WorkerExecutor executor;

    public static final String COMPILE = "compile";
    public static final String PROVIDED = "provided";
    public static final String SYSTEM = "system";
    public static final String RUNTIME = "runtime";
    public static final String TEST = "test";

    final static String NEW_BRANCH = "\\+\\-\\s|\\\\\\-\\s";
    final static String EXISTING_BRANCH = "\\|\\s{2}";
    final static String SPACER = "\\s{3}";
    final static String TREE_LEVEL_REGEX = "((?:" + NEW_BRANCH + "|" + EXISTING_BRANCH + "|" + SPACER + ")+)";
    final static String ARTIFACT_TREE_MODULE_REGEX = "\\[INFO\\]\\s((\\S+:){3,4}\\S+).*";
    final static String ARTIFACT_TREE_DEPENDENCY_REGEX = "\\[INFO\\]\\s" + TREE_LEVEL_REGEX + "((\\S+:){4,5}\\S+).*";
    final static String ARTIFACT_EXTRACTION_ERROR_REGEX = "\\[ERROR\\]\\s.*";
    final static String ARTIFACT_TREE_NOT_RECOGNISED_REGEX = "\\[INFO\\]\\s" + TREE_LEVEL_REGEX + ".*";
    public static final String DEFAULT_PATH = "output/dependencies";

    @Override
    protected String[] features() {
        return new String[] { "mavenDependencyParse" };
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    @Override
    protected void taskStart(Promise<?> taskPromise) {
        Integer coresCount = CpuCoreSensor.availableProcessors();
        LOG.debug("Detected {} CPU cores, using all of them", coresCount);
        executor = vertx.createSharedWorkerExecutor("maven-dependency-import-worker-pool"
                , coresCount
                , 60
                , TimeUnit.SECONDS);

        BiFunction<Graph, JsonObject, List<JsonObject>> componentExtractor = AbstractAggregationTaskVerticle::getMavenDependencyComponents;
        @SuppressWarnings("rawtypes")
        List<Future> futures = processGraph(this::aggregateDomainData, c -> processDependencyTree(c, executor), componentExtractor);
        completeCompositeTask(futures, taskPromise);
    }

    @SuppressWarnings("rawtypes")
    private List<Future> aggregateDomainData(JsonObject jsonObject) {
        Future future = Future.succeededFuture();
        return Collections.singletonList(future);
    }

    @SuppressWarnings("rawtypes")
    private List<Future> processDependencyTree(JsonObject component, WorkerExecutor executor) {
        LOG.debug("{}: Scheduling blocking execution of maven dependencies import to the graph", component.getString(F_ID));
        Future blockingFuture = executor.executeBlocking(() -> processDependencies(component), false);
        return Collections.singletonList(blockingFuture);
    }

    private Callable<Void> processDependencies(JsonObject component) {
        long executionStart = System.currentTimeMillis();
        File depTreeFile = Path.of(component.getString(F_DIRECTORY)).resolve("target").resolve("dependency_tree.json").toFile();
        if (depTreeFile.exists()) {
            parseDepTreeFromCi(component, depTreeFile);
        } else {
            parseDepFromMaven(component);
        }
        LOG.debug("{}: Dependency import finished in {}ms", component.getString(F_ID),
                System.currentTimeMillis() - executionStart);
        return null;
    }

    //------------------------------------------------------
    // Parse "output/dependencies/${component.id}_dependency.txt" from MavenDependencyDumpExtractVerticle

    private void parseDepFromMaven(JsonObject component) {
        List<JsonObject> rawDependencies = getDependenciesFromDump(component);
        if (! rawDependencies.isEmpty()) {
            processDependencies(component, rawDependencies);
        }
    }

    private void processDependencies(JsonObject component, List<JsonObject> rawDependencies) {
        JsonObject source;
        Integer lastLevel = 0;
        JsonObject lastDestination = component;
        LinkedList<JsonObject> stack = new LinkedList<>();
        Graph graph = this.graph;
        String compId = component.getString(F_ID);
        for (JsonObject destination : rawDependencies) {
            generateId(destination);
            Integer targetLevel = destination.getInteger("level");
            String scope = destination.getString("scope");

            stripExcessiveFields(destination);
            JsonObject dependencyEdge;
            if (targetLevel.equals(1)) {
                dependencyEdge = new JsonObject()
                        .put("type", "module")
                        .put("component", compId);
            } else {
                dependencyEdge = new JsonObject()
                        .put("type", "dependency")
                        .put("scope", scope)
                        .put("component", compId);
            }

            if (targetLevel > lastLevel) { // going up
                source = lastDestination;
                if (targetLevel - lastLevel > 1) {
                    LOG.error("{}: Error during parsing the dependency tree: potentially missed entry"
                        + " before the line between {} and {}. Levels {}-{}",
                            compId, source.getString(F_ID),
                            destination.getString(F_ID), lastLevel, targetLevel);
                }
                stack.push(source);
            } else if (targetLevel == lastLevel) { // same level
                source = stack.peek();
            } else { //going down
                for (int i = 0 ; i < lastLevel - targetLevel; i++) {
                    stack.remove();
                }
                source = stack.peek();
            }

            lastDestination = destination;
            lastLevel = targetLevel;
            graph.addEdge(source, destination, dependencyEdge);
        }
    }

    private void stripExcessiveFields(JsonObject destination) {
        destination.remove("scope");
        destination.remove("level");
    }

    private void generateId(JsonObject destination) {
        destination.put(F_ID, new StringBuffer()
                .append(destination.getString("groupId")).append(":")
                .append(destination.getString("artifactId")).append(":")
                .append(destination.getString("package")).append(":")
                .append(destination.getString("version"))
                .toString()
        );
    }

    private List<JsonObject> getDependenciesFromDump(JsonObject component) {
        File pomFile = Path.of(component.getString(F_DIRECTORY)).resolve("pom.xml").toFile();
        if (!pomFile.exists()) {
            return Collections.emptyList();
        }

        String compId = component.getString(F_ID);
        Path dependencyDumpFile = Path.of(DEFAULT_PATH, compId + "_dependency.txt");
        if (! dependencyDumpFile.toFile().exists()) {
            return Collections.emptyList();
        }

        List<JsonObject> result = new ArrayList<>();
        try (Stream<String> stream = Files.lines(dependencyDumpFile)) {
            stream.forEach(str -> processArtifact(str, result, component));
        } catch (IOException e) {
            report.exceptionThrown(component, e);
            return Collections.emptyList();
        }

        List<String> errors = result.stream()
                .filter(o -> o.containsKey("error"))
                .map(o -> o.getString("error").replaceAll("\\p{Cntrl}", ""))
                .collect(Collectors.toList());
        if (errors.size() > 0) {
            LOG.error("{}: Dump extraction failed", compId);
            report.addMessage("ERROR", component, StringUtils.join(errors, "\n"));
        }

        LOG.debug("{}: Dump extraction finished. Received {} entries", compId, result.size());

        return result.stream()
                .filter(o -> !o.containsKey("error"))
                .collect(Collectors.toList());
    }

    private void processArtifact(String line, List<JsonObject> result, JsonObject component) {
        if (line.matches(ARTIFACT_EXTRACTION_ERROR_REGEX)) {
            result.add(new JsonObject().put("error", line));
        }
        if (line.matches(ARTIFACT_TREE_DEPENDENCY_REGEX)) {
            String dependencyLevel = line.replaceAll(ARTIFACT_TREE_DEPENDENCY_REGEX,"$1");
            String artifactLine = line.replaceAll(ARTIFACT_TREE_DEPENDENCY_REGEX,"$2");
            String[] artifact = artifactLine.split(":");

            result.add(new JsonObject()
                    .put(F_ID, "")  // Let it be the first in file. See generateId()
                    .put("artifactId", artifact[1])
                    .put("groupId", artifact[0])
                    .put("package", artifact[2])
                    .put("version", artifact.length == 6 ? artifact[3] + ":" + artifact[4] : artifact[3])
                    .put("scope", artifact.length == 6 ? artifact[5] : artifact[4])
                    .put("level", 1 + dependencyLevel.length() / 3)
                    .put("type", "library")
            );
        } else if (line.matches(ARTIFACT_TREE_MODULE_REGEX)) {
            String artifactLine = line.replaceAll(ARTIFACT_TREE_MODULE_REGEX,"$1");
            String[] artifact = artifactLine.split(":");

            result.add(new JsonObject()
                    .put(F_ID, "")
                    .put("artifactId", artifact[1])
                    .put("groupId", artifact[0])
                    .put("package", artifact[2])
                    .put("version", artifact.length == 5 ? artifact[3] + ":" + artifact[4] : artifact[3])
                    .put("scope", "compile")
                    .put("level", 1)
                    .put("type", "library")
            );
        } else if (line.matches(ARTIFACT_TREE_NOT_RECOGNISED_REGEX)){
            report.addMessage("ERROR", component, "Dependency tree element not recognized: " + line);
        }
    }

    //------------------------------------------------------
    // Parse "${component.directoryPath}/target/dependency_tree.json} from CI pipeline.

    private void parseDepTreeFromCi(JsonObject component, File depTreeFile) {
        try {
            JsonObject depTree = JsonUtils.readJsonFile(depTreeFile.toString());
            JsonArray modules = depTree.getJsonArray("modules");
            if (modules == null) {
                return;
            }
            String compId = component.getString(F_ID);
            for (Object o1: modules) {
                JsonObject module = (JsonObject) o1;
                String projectId = module.getString("id");  // *Not* Graph.F_ID

                JsonObject moduleEdge = new JsonObject()
                        .put("type", "module")
                        .put("component", compId);
                graph.addEdge(component, artifactIdToVertex(projectId), moduleEdge);

                JsonArray deps = module.getJsonArray("dependencies");
                if (deps == null) {
                    continue;
                }
                for (Object o2: deps) {
                    JsonObject depEntry = (JsonObject) o2;
                    String artifactFrom = depEntry.getString("from");
                    String artifactTo = depEntry.getString("to");
                    JsonObject dependencyEdge = new JsonObject()
                            .put("type", "dependency")
                            .put("scope", depEntry.getString("scope"))
                            .put("component", compId);
                    graph.addEdge(artifactIdToVertex(artifactFrom), artifactIdToVertex(artifactTo), dependencyEdge);
                }
            }
        } catch (Exception e) {
            report.exceptionThrown(component, e);
        }
    }

    private JsonObject artifactIdToVertex(String artifactId) {
        JsonObject existing = graph.getVertex(artifactId);
        if (existing != null) {
            return existing;
        }
        String[] parts = artifactId.split(":");
        return new JsonObject()
            .put(F_ID, artifactId)
            .put("groupId", parts[0])
            .put("artifactId", parts[1])
            .put("package", parts[2])
            .put("version", parts[3])
            .put("type", "library");
    }
}
