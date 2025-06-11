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

/**
 * <p>Create transitive queue dependencies:
 * <p>Component A has maven dependency on library B, library B depends on (produces/consumes) queue C
 * =&gt; Create queue dependency from component A to queue C if it does not exist.
 *
 * <p>USAGE: Needed for both processing set of components in one pass (aka desktop run) and assembly.
 */
public class CreateTransitiveQueueDependenciesTask implements GraphProcessorTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreateTransitiveQueueDependenciesTask.class);

    /** {@inheritDoc} */
    public void process(Graph graph) {
        long startTime = System.nanoTime();
        LOGGER.info("Starting task");

        List<Map<String, JsonObject>> transitiveConsumers = graph.traversal().V("Infra")
                .out().hasType("mq").as("MQ")
                .outE("consumer").as("QE")
                .inV().as("C1")
                .in("library").as("C2")
                .<JsonObject>select("MQ", "QE", "C1", "C2")
                .toList();

        for (Map<String, JsonObject> tuple: transitiveConsumers) {
            JsonObject mq = tuple.get("MQ");
            JsonObject queueEdge = tuple.get("QE");
            String queueName = queueEdge.getString(F_NAME);
            JsonObject transitiveConsumer = tuple.get("C2");
            List<JsonObject> connectingEdges = graph.getEdgesBetween(mq, transitiveConsumer);
            if (connectingEdges.stream().noneMatch(
                    edge -> edge.getString(F_TYPE).equals("consumer") && edge.getString(F_NAME).equals(queueName)))
            {
                JsonObject newEdge = new JsonObject()
                        .put(F_TYPE, "consumer")
                        .put(F_NAME, queueName)
                        .put("reference", "transitive")
                        .put("source", tuple.get("C1").getString(F_ID));
                LOGGER.debug("Adding transitive MQ dependency: from={}, to={}, edge={}",
                        mq.getString(F_ID), transitiveConsumer.getString(F_ID), newEdge);
                graph.addEdge(mq, transitiveConsumer, newEdge);
            }
        }

        List<Map<String, JsonObject>> transitiveProducers = graph.traversal().V("Infra")
                .out().hasType("mq").as("MQ")
                .inE("producer").as("QE")
                .outV().as("C1")
                .in("library").as("C2")
                .<JsonObject>select("MQ", "QE", "C1", "C2")
                .toList();

        for (Map<String, JsonObject> tuple: transitiveProducers) {
            JsonObject mq = tuple.get("MQ");
            JsonObject queueEdge = tuple.get("QE");
            String queueName = queueEdge.getString(F_NAME);
            JsonObject transitiveProducer = tuple.get("C2");
            List<JsonObject> connectingEdges = graph.getEdgesBetween(transitiveProducer, mq);
            if (connectingEdges.stream().noneMatch(
                    edge -> edge.getString(F_TYPE).equals("producer") && edge.getString(F_NAME).equals(queueName)))
            {
                JsonObject newEdge = new JsonObject()
                        .put(F_TYPE, "producer")
                        .put(F_NAME, queueName)
                        .put("reference", "transitive")
                        .put("source", tuple.get("C1").getString(F_ID));
                LOGGER.debug("Adding transitive MQ dependency: from={}, to={}, edge={}",
                        transitiveProducer.getString(F_ID), mq.getString(F_ID), newEdge);
                graph.addEdge(transitiveProducer, mq, newEdge);
            }
        }

        long endTime = System.nanoTime();
        LOGGER.info("Task completed in {}", Duration.ofNanos(endTime - startTime));
    }

}

