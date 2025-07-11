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
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;
import org.apache.maven.shared.invoker.*;
import org.qubership.itool.modules.graph.Graph;
import org.qubership.itool.utils.JsonUtils;
import org.qubership.itool.utils.XmlParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

/**
 * Run maven and collect dependency tree to "output/dependencies/${component.id}_dependency.txt"
 */
public class MavenDependencyDumpExtractVerticle extends AbstractAggregationTaskVerticle {
    protected Logger LOG = LoggerFactory.getLogger(MavenDependencyDumpExtractVerticle.class);

    public static final String DEFAULT_PATH = "output/dependencies";
    private final XPathFactory xPathfactory = XPathFactory.newInstance();
    private final XPath xpath = xPathfactory.newXPath();
    // Have to be global to avoid closing by GC
    private WorkerExecutor executor;
    
    @Override   
    protected String[] features() {
        return new String[] { "mavenDependency" };
    }
    
    @Override
    protected void taskStart(Promise<?> taskPromise) {
        int coresCount = CpuCoreSensor.availableProcessors();
        LOG.debug("Detected {} CPU cores, using all of them", coresCount);
        executor = vertx.createSharedWorkerExecutor("maven-dependency-extraction-worker-pool"
                , coresCount
                , 60
                , TimeUnit.MINUTES);
        BiFunction<Graph, JsonObject, List<JsonObject>> componentExtractor = AbstractAggregationTaskVerticle::getMavenDependencyComponents;
        List<Future> futures = processGraph(this::aggregateDomainData, c -> processDependencyTree(c, executor), componentExtractor);
        completeCompositeTask(futures, taskPromise);
    }

    @SuppressWarnings("rawtypes")
    private List<Future> processDependencyTree(JsonObject component, WorkerExecutor executor) {
        File depTreeFile = Path.of(component.getString("directoryPath")).resolve("target").resolve("dependency_tree.json").toFile();
        if (depTreeFile.exists()) {
            LOG.info("File {} already exists for component {}, no need to run maven", depTreeFile, component.getString(Graph.F_ID));
            return Collections.emptyList();
        }
        String pomPath = component.getString("directoryPath");
        LOG.debug("{}: Scheduling blocking execute of maven dependencies collection, root pom path {}", component.getString("name"), pomPath);
        Future blockingFuture = executor.executeBlocking(() -> processDependencies(component), false);
        return Collections.singletonList(blockingFuture);
    }

    private Callable<Void> processDependencies(JsonObject component) {
        long executionStart = System.nanoTime();
        createMavenDump(component);
        LOG.debug("{}: Dependency retrieval finished in {}", component.getString(Graph.F_ID), Duration.ofNanos(System.nanoTime() - executionStart).toString());
        return null;
    }

    private void createMavenDump(JsonObject component) {
        File pomFile = Path.of(component.getString("directoryPath")).resolve("pom.xml").toFile();
        if (!pomFile.exists()) {
            LOG.info("Pom file {} was not found", pomFile.getPath());
            return;
        }
        try {
            List<String> moduleLocations = new ArrayList<>();
            moduleLocations.add("/project/profiles/profile/modules/*");
            moduleLocations.add("/project/modules/*");

            List<String> modules = extractProperties(pomFile, moduleLocations);
            for (String module : modules) {
                File modulePomFile = Path.of(component.getString("directoryPath")).resolve(module).resolve("pom.xml").toFile();
                List<String> artifactIdLocation = new ArrayList<>();
                artifactIdLocation.add("/project/build/plugins/plugin/artifactId");
                List<String> artifactIds = extractProperties(modulePomFile, artifactIdLocation);
                if (artifactIds.contains("frontend-maven-plugin")) {
                    LOG.info("skipping entire component {} because of its module {} uses frontend-maven-plugin", component.getString(Graph.F_ID), module);
                    return;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        Path dependencyDumpPath = Path.of(DEFAULT_PATH);
        File dir = dependencyDumpPath.toFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String compId = component.getString(Graph.F_ID);
        File dependencyDumpFile = dependencyDumpPath.resolve(compId + "_dependency.txt").toFile();
        InvocationRequest request = new DefaultInvocationRequest();
        request.setPomFile(pomFile);
        request.setBatchMode(true);
        List<String> goals = new ArrayList<>();
        String mavenGoals = (String)JsonPointer.from("/maven/dependency").queryJson(component);
        if (mavenGoals != null) {
            for (String g : mavenGoals.split(" ")) {
                goals.add(g);
            }
        } else {
            goals.add("dependency:tree");
        }
        request.setGoals(goals);
        Invoker invoker = new DefaultInvoker();
        InvocationResult invocationResult;
        try (PrintWriter dependencyTreeWriter = new PrintWriter(new BufferedWriter(new FileWriter(dependencyDumpFile, JsonUtils.UTF_8)))) {
            request.setOutputHandler(str -> writeToFile(str, dependencyTreeWriter));
            invocationResult = invoker.execute(request);
        } catch (Exception e) {
            report.exceptionThrown(component, e);
            return;
        }
        if (invocationResult.getExitCode() != 0) {
            report.addMessage("ERROR", component,
                      compId + ": Dependency extraction failed with exit code "
                    + invocationResult.getExitCode() + ". Result can be found here: "
                    + dependencyDumpFile.getAbsolutePath());
        } else {
            LOG.debug("{}: Extraction finished. Result is stored in {} ", compId, dependencyDumpFile.getAbsolutePath());
        }

    }

    private void writeToFile(String line, PrintWriter dump) {
        dump.println(line);
    }

    @SuppressWarnings("rawtypes")
    private List<Future> aggregateDomainData(JsonObject jsonObject) {
        Future future = Future.succeededFuture();
        return Collections.singletonList(future);
    }

    private List<String> extractProperties(File file, List<String> locations) throws Exception {
        Document document = XmlParser.parseXmlFile(String.valueOf(file));
        List<String> properties = new ArrayList<>();
        for (String location : locations) {
            NodeList props = null;
            try {
                props = (NodeList) xpath.compile(location).evaluate(document, XPathConstants.NODESET);
                for (int i = 0; i < props.getLength(); i++) {
                    Element item = (Element) props.item(i);
                    properties.add(item.getTextContent());
                }
            } catch (XPathExpressionException e) {
                throw new RuntimeException(e);
            }
        }
        return properties;
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }
}
