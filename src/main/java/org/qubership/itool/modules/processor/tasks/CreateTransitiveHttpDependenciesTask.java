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

package org.qubership.itool.modules.processor.tasks;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.qubership.itool.modules.graph.Graph;

import io.vertx.core.json.JsonObject;

import static org.qubership.itool.modules.graph.Graph.*;
import static org.qubership.itool.modules.gremlin2.P.*;
import static org.qubership.itool.modules.gremlin2.graph.__.select;

/**
 * <p>Create transitive HTTP dependencies:
 * <p>Component A has maven dependency on library B, library B has http dependency on component C
 * =&gt; Create http dependency from component A to component C if it does not exist.
 *
 * <p>USAGE: Needed for both processing set of components in one pass (aka desktop run) and assembly.
 */
public class CreateTransitiveHttpDependenciesTask implements GraphProcessorTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreateTransitiveHttpDependenciesTask.class);

    /** {@inheritDoc} */
    public void process(Graph graph) {
        long startTime = System.nanoTime();
        LOGGER.info("Starting task");

        List<Map<String, JsonObject>> libToHttpDeps = graph.traversal().V().hasType("domain")
                .out().as("COMP")
                .out("library").as("LIB")
                .outE("mandatory", "optional", "startup").as("E")
                .inV().as("DEP").has(F_ID, neq(select("COMP").id()))
                .<JsonObject>select("COMP", "LIB", "E", "DEP")
                .toList();

        for (Map<String, JsonObject> tuple: libToHttpDeps) {
            JsonObject from = tuple.get("COMP");
            JsonObject depEdge = tuple.get("E");
            String depType = depEdge.getString(F_TYPE);
            JsonObject dependency = tuple.get("DEP");
            List<JsonObject> connectingEdges = graph.getEdgesBetween(from, dependency);
            if (connectingEdges.stream().noneMatch( edge -> edge.getString(F_TYPE).equals(depType) )) {
                JsonObject newEdge = new JsonObject()
                        .put(F_TYPE, depType)
                        .put("reference", "transitive")
                        .put("source", tuple.get("LIB").getString(F_ID));
                LOGGER.debug("Adding transitive HTTP dependency: from={}, to={}, edge={}",
                        from.getString(F_ID), dependency.getString(F_ID), newEdge);
                graph.addEdge(from, dependency, newEdge);
            }
        }

        long endTime = System.nanoTime();
        LOGGER.info("Task completed in {}", Duration.ofNanos(endTime - startTime));
    }

}

