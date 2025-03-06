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

package org.qubership.itool.tasks.parsing;

import org.qubership.itool.tasks.FlowTask;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.WorkerExecutor;
import io.vertx.core.impl.cpu.CpuCoreSensor;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.camel.util.AntPathMatcher;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.qubership.itool.modules.report.GraphReport;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.qubership.itool.modules.graph.Graph.F_DIRECTORY;
import static org.qubership.itool.modules.graph.Graph.F_ID;
import static org.qubership.itool.modules.graph.Graph.V_DOMAIN;
import static org.qubership.itool.modules.graph.Graph.V_ROOT;

public abstract class AbstractInclusiveParseFileTask extends FlowTask {

    protected static final String LINE_BREAK_REGEX = "[\\n\\r]{1,2}";
    protected static final Pattern LINE_BREAK_PATTERN = Pattern.compile(LINE_BREAK_REGEX);

    @Override
    protected void taskStart(Promise<?> taskPromise) throws Exception {
        Integer coresCount = CpuCoreSensor.availableProcessors();
        WorkerExecutor executor = vertx.createSharedWorkerExecutor("parsing-worker-pool"
                , coresCount
                , 10
                , TimeUnit.MINUTES);

        vertx.executeBlocking(promise -> {
                    List<Future> futureList = parseFiles(executor, getFilePatterns());
                    joinFuturesAndHandleResult(futureList)
                            .onComplete(ar -> promise.complete());
                })
                .onComplete(res -> taskCompleted(taskPromise));
    }


    /**
     * Get file patterns to scan.
     * Recognized pattern types:
     * <ul><li>Relative paths to single file like "a/b/c.d" or "c.d"
     * <li>File name patterns like "*.java" (matched in all directories)
     * <li>ANT matcher patterns like "deployments/<code>**</code>/*.yaml"
     * </ul>
     * @return File patterns
     */
    protected abstract String[] getFilePatterns();

    protected abstract void parseSingleFile(JsonObject domain, JsonObject component, String fileName)
            throws IOException;


    @SuppressWarnings("rawtypes")
    protected List<Future> parseFiles(WorkerExecutor executor, String ... filePatterns) {

        // Pre-parse patterns
        List<String> simplePatterns = new ArrayList<>();
        List<Pattern> shallowPatterns = new ArrayList<>();
        List<String> deepPatterns = new ArrayList<>();

        for (String filePattern: filePatterns) {
            if (!filePattern.contains("*") && !filePattern.contains("?")) {
                simplePatterns.add(filePattern);
            } else if (!filePattern.contains("/")) {
                Pattern regex = Pattern.compile(
                        (filePattern.startsWith("*.") ? "^." : "^") // Pattern "*.ext" requires non-empty part before dot
                                + filePattern.replace(".", "\\.").replace("*", ".*")
                                + (filePattern.endsWith(".*") ? ".$" : "$")); // Pattern "name.*" requires non-empty extension
                shallowPatterns.add(regex);
            } else {
                deepPatterns.add(filePattern);
            }
        }

        // Find components
        List<Future> futures = new ArrayList<>();
        List<Map<String, JsonObject>> componentsWithDomains = getComponentsWithDomains();

        for (Map<String, JsonObject> componentWithDomain : componentsWithDomains) {
            JsonObject domain = componentWithDomain.get("D");
            JsonObject component = componentWithDomain.get("C");
            getLogger().debug("Queue the parsing of files for {} component", component.getString("id"));

            // Async parallel executions: one Future task per component. Scan files, then read needed ones.
            Future future = executor.executeBlocking(promise -> {
                long startTime = System.nanoTime();
                List<String> pathList = findAllFiles(component, simplePatterns, shallowPatterns, deepPatterns);
                for (String fileName: pathList) {
                    try {
                        parseSingleFile(domain, component, fileName);
                    } catch (Exception /*| DecodeException*/ e) {
                        this.report.addMessage(
                                GraphReport.EXCEPTION, component,
                                "Parsing of file " + fileName + " failed:\n" + ExceptionUtils.getStackTrace(e));
                    }
                }
                long endTime = System.nanoTime();
                long processingTime = endTime - startTime;
                getLogger().debug("Processing time for component " + component.getValue("id") + ": " + Duration.ofNanos(processingTime));

                promise.complete();
            }, false);
            futures.add(future);
        }

        return futures;
    }

    protected List<Map<String, JsonObject>> getComponentsWithDomains() {
        return V(V_ROOT).out().hasType(V_DOMAIN).as("D")
                .out().hasKeys(F_DIRECTORY).as("C")
                .<JsonObject>select("D", "C").toList();
    }

    protected List<String> findAllFiles(JsonObject component,
                                        List<String> simplePatterns, List<Pattern> shallowPatterns, List<String> deepPatterns) {

        List<String> result = new ArrayList<>();
        String directoryPath = component.getString(F_DIRECTORY);

        JsonArray excludeDirs = component.getJsonArray("excludeDirs");
        Set<Path> topDirExcludes = excludeDirs==null || excludeDirs.isEmpty() ? Collections.emptySet()
                : excludeDirs.stream()
                .map(s -> Path.of( (String)s ))
                .collect(Collectors.toSet());

        for (String filePattern : simplePatterns) {
            if (!isExcluded(topDirExcludes, Path.of(filePattern), Path.of(directoryPath))) {
                File file = new File(directoryPath, filePattern);
                if (file.isFile()) {
                    result.add(file.getPath());
                }
            }
        }

        if (shallowPatterns.isEmpty() && deepPatterns.isEmpty()) {
            return result;
        }

        Path basePath = FileSystems.getDefault().getPath(directoryPath);
        PathAccumulatorVisitor<Path> visitor = new PathAccumulatorVisitor<>(basePath, shallowPatterns, deepPatterns, topDirExcludes);
        try {
            Files.walkFileTree(basePath, visitor);
        } catch (UncheckedIOException|IOException e) {
            report.addMessage(GraphReport.EXCEPTION, component,
                    "Critical failure during file walking procedure:\n" + ExceptionUtils.getStackTrace(e));
        }
        visitor.getPaths().forEach(path -> result.add(path.toString()));

        getLogger().trace("{}: Found files {}", component.getString(F_ID), result);
        return result;
    }

    protected boolean isExcluded(Collection<Path> topDirExcludes, Path relativePath, Path fileDirectory) {
        for (Path topDirExclude : topDirExcludes) {
            if (relativePath.startsWith(topDirExclude)) {
                getLogger().trace("Excluding file {} belonging to a subComponent directory {}", relativePath, topDirExclude);
                return true;
            }
        }
        return false;
    }

    class PathAccumulatorVisitor<T extends Path> extends SimpleFileVisitor<T> {
        List<T> paths = new ArrayList<>();
        List<Pattern> shallowPatterns;
        List<String> deepPatterns;
        Collection<Path> topDirExcludes;
        Path basePath;

        public PathAccumulatorVisitor(Path basePath, List<Pattern> shallowPatterns, List<String> deepPatterns, Collection<Path> topDirExcludes) {
            this.basePath = basePath;
            this.shallowPatterns = shallowPatterns;
            this.deepPatterns = deepPatterns;
            this.topDirExcludes = topDirExcludes;
        }

        public List<T> getPaths() {
            return paths;
        }

        @Override
        public FileVisitResult visitFile(T file, BasicFileAttributes attrs) throws IOException {
            super.visitFile(file, attrs);
            if (!attrs.isRegularFile()) {
                return FileVisitResult.CONTINUE;
            }

            for (Pattern regex : shallowPatterns) {
                if (regex.matcher(file.getFileName().toString()).matches()) {
                    paths.add(file);
                    return FileVisitResult.CONTINUE;
                }
            }

            if (deepPatterns.isEmpty()) {
                return FileVisitResult.CONTINUE;
            }
            Path relativePath = basePath.relativize(file);
            String relativePathString = relativePath.toString().replace('\\', '/');
            for (String deepPattern : deepPatterns) {
                // Implementation of ANT path matcher from Camel has no pre-compilation,
                // yet it is still faster than regexp matched against full path.
                if (AntPathMatcher.INSTANCE.match(deepPattern, relativePathString)) {
                    paths.add(file);
                    return FileVisitResult.CONTINUE;
                }
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(T file, IOException exc) throws IOException
        {
            report.addMessage(GraphReport.EXCEPTION,
                    new JsonObject().put("id", "iTool"),
                    "File walking failure during attempt to visit file " + file.toString()
                            + ":\n" + ExceptionUtils.getStackTrace(exc));
            // Keep walking
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult preVisitDirectory(T dir, BasicFileAttributes attrs) throws IOException
        {
            super.preVisitDirectory(dir, attrs);
            Path relativePath = basePath.relativize(dir);
            if (isExcluded(topDirExcludes, relativePath, dir)) {
                return FileVisitResult.SKIP_SUBTREE;
            }
            return FileVisitResult.CONTINUE;
        }
    }
}
