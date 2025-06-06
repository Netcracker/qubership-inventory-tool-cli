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
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.qubership.itool.modules.graph.Graph;
import org.qubership.itool.modules.processor.matchers.TargetMocksMatcher;
import org.qubership.itool.modules.report.GraphReport;
import org.qubership.itool.modules.report.GraphReportImpl;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import static org.qubership.itool.modules.graph.GraphDataConstants.*;
import static org.qubership.itool.modules.gremlin2.P.*;
import static org.qubership.itool.modules.graph.Graph.*;

/**
 * <p>Recreate HTTP dependencies to existing components or their mocks that were NOT created by
 * SetEdgesBetweenComponentsVerticle in qubership-inventory-tool-cli v1.0.0
 * <p>Component A has declared dependency on component B by dns name
 * =&gt; Find a component vertex with appropriate "/details/dnsName" (or create a mock if it does not exist)
 * and create http dependency from component A to that vertex if it does not exist.
 *
 * <p>Also, this task cleans up error report removing messages about missing http dependencies
 * that have been recovered during merge (see also: {@link TargetMocksMatcher})
 *
 * <p>USAGE: Needed during assembly to fix up older artifacts and clean up error report.
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class RecreateHttpDependenciesTask implements GraphProcessorTask {

    static final Logger LOG = LoggerFactory.getLogger(RecreateHttpDependenciesTask.class);

    static final Pattern VALID_DNS_NAMES_PATTERN = Pattern.compile("^[-a-zA-Z0-9]+$");

    public void process(Graph graph) {
        long startTime = System.nanoTime();
        LOG.info("Starting task");

        List<Map<Object, Object>> componentsWithRawDependencies = graph.traversal()
            .V().hasType("domain").out()
            .values(
                "C:/",
                // Duplicates COMP_DEPENDENCY_TYPES a little
                "startup:/details/dependencies/startup",
                "mandatory:/details/dependencies/mandatory",
                "optional:/details/dependencies/optional")
            .toList();

        // Collect a Set of all DNS names required by components in graph
        Set<String> requiredDnsNames = collectRequiredDnsNames(componentsWithRawDependencies);

        /* Find all vertices (mocks are non-components, that is, they do not belong to any
         * domain or application) having DNS names from that Set.
         */
        Map<String, JsonObject> dnsNameToComponentMap = findComponentVerticesByDnsNames(graph, requiredDnsNames);

        recreateMissingDependencyEdges(graph, componentsWithRawDependencies, dnsNameToComponentMap);

        long endTime = System.nanoTime();
        LOG.info("Task completed in {}", Duration.ofNanos(endTime - startTime));
    }

    private void recreateMissingDependencyEdges(Graph graph, List<Map<Object, Object>> componentsWithRawDependencies, Map<String, JsonObject> dnsNameToComp) {
        for (Map<Object, Object> item: componentsWithRawDependencies) {
            JsonObject component = (JsonObject) item.get("C");
            for (String dependencyType: COMP_DEPENDENCY_TYPES.keySet()) {
                JsonArray dependencyDnsNames = getJsonArray(item, dependencyType);
                if (dependencyDnsNames == null) {
                    continue;
                }
                for (Object dnsName: dependencyDnsNames) {
                    if (NOS_TO_RECOGNIZE.contains(dnsName)) {
                        continue;
                    }
                    JsonObject targetComponent = dnsNameToComp.computeIfAbsent((String)dnsName, newDnsName -> createMockByDnsName(graph, newDnsName));
                    if (isEdgeMissing(graph, component, dependencyType, targetComponent)) {
                        JsonObject edge = new JsonObject()
                            .put(F_TYPE, dependencyType)
                            .put("protocol", "http");
                        graph.addEdge(component, targetComponent, edge);
                        LOG.info("Edge recreated: from='{}', to='{}', edge='{}'",
                            component.getString(F_ID), targetComponent.getString(F_ID), edge);
                    }
                }
            }
        }
    }

    private boolean isEdgeMissing(Graph graph, JsonObject component, String dependencyType, JsonObject targetComponent) {
        return graph.getVertex(targetComponent.getString(F_ID)) == null
            || graph.getEdgesBetween(component, targetComponent).stream()
                .noneMatch(edge -> dependencyType.equals(edge.getString(F_TYPE)));
    }

    private Set<String> collectRequiredDnsNames(List<Map<Object, Object>> componentsWithRawDependencies) {
        Set<String> requiredDnsNames = componentsWithRawDependencies.stream()
            .flatMap(item -> concatStreams(item))
            .collect(Collectors.toSet());
        requiredDnsNames.removeIf(dnsName ->
                   dnsName == null
                || NOS_TO_RECOGNIZE.contains(dnsName)
                || ! VALID_DNS_NAMES_PATTERN.matcher(dnsName).matches());
        return requiredDnsNames;
    }

    private Map<String, JsonObject> findComponentVerticesByDnsNames(Graph graph, Set<String> requiredDnsNames) {
        Map<String, JsonObject> dnsNameToComp = new HashMap<>();
        List<Map<String, Object>> componentsWithDnsNames = graph.traversal().V()
                .hasKey(P_DETAILS_DNS_NAMES).as("C")
                .values("dnsNames:/details/dnsNames").unfold().as("DN")
                .select("C", "DN").has("DN", within(requiredDnsNames.toArray())).toList();

        for (Map<String, Object> entry: componentsWithDnsNames) {
            JsonObject component = (JsonObject) entry.get("C");
            String dnsName = (String) entry.get("DN");
            JsonObject oldComponent = dnsNameToComp.put(dnsName, component);
            GraphReport report = graph.getReport();
            if (oldComponent != null) {
                String msg = "Vertices '" + oldComponent.getString(F_ID) + "' and '" + component.getString(F_ID)
                        + "' share the same dnsName '" + dnsName + "'";
                if (report != null) {
                    report.addMessage(GraphReport.CONF_ERROR, component, msg);
                } else {
                    LOG.error(msg);
                }
            }

            // TODO: Move the cleanup outside, it shouldn't be the part of searching process
            if (component.getBoolean(F_MOCK_FLAG, false) == false && report instanceof GraphReportImpl) {
                // Remove records reported by current component related to found non-mock component.
                JsonArray records = ((GraphReportImpl)report).getRecords();
                removeOutdatedReportRecords(component, dnsName, records);
            }
        }

        return dnsNameToComp;
    }

    void removeOutdatedReportRecords(JsonObject comp, String dnsName, JsonArray reportRecords) {
        Pattern pattern = Pattern.compile("^Reference was not found. Reference: \\w+ http dependency " + dnsName + "$");
        synchronized(reportRecords) {
            Iterator<?> it = reportRecords.iterator();
            while (it.hasNext()) {
                Object o = it.next();
                if (! (o instanceof JsonObject)) {
                    continue;
                }
                JsonObject record = (JsonObject) o;
                String message = record.getString("message");
                boolean matchingErrorFound = GraphReport.CONF_ERROR.equals(record.getString("type"))
                        && message != null
                        && message.startsWith("Reference was not found. Reference:")
                        && pattern.matcher(message).matches(); // TODO: Move pattern to static field and update logic
                if (matchingErrorFound) {
                    it.remove();
                    LOG.info("Dependency on dnsName {} from {} has been restored to {}. Report message removed: '{}'",
                            dnsName, record.getString("component"), comp.getString(F_ID), message);
                }
            }
        }
    }

    JsonObject createMockByDnsName(Graph graph, String dnsName) {
        String mockId = "mock:dnsName:" + dnsName;
        LOG.info("Mock vertex '{}' recreated to substitute dnsName '{}'", mockId, dnsName);
        JsonObject mockVertex = new JsonObject()
            .put(F_ID, mockId)
            .put(F_TYPE, V_UNKNOWN)
            .put(F_NAME, dnsName)
            .put(F_MOCK_FLAG, true)
            .put(F_MOCKED_FOR, new JsonArray().add(P_DETAILS_DNS_NAMES))
            .put(F_DETAILS, new JsonObject()
                    .put(F_DNS_NAME, dnsName)
                    .put(F_DNS_NAMES, dnsName)
            )
            ;
        graph.addVertex(mockVertex);
        return mockVertex;
    }

    static Stream<String> concatStreams(Map<Object, Object> item) {
        Stream<String> newStream = Stream.empty();
        for (String depType : COMP_DEPENDENCY_TYPES.keySet()) {
            JsonArray asArray = getJsonArray(item, depType);
            if (asArray != null) {
                newStream = Stream.concat(newStream, (Stream)asArray.stream());
            }
        }
        return newStream;
    }

    static JsonArray getJsonArray(Map<Object, Object> item, String key) {
        Object value = item.get(key);
        if (value instanceof List) {
            return new JsonArray((List)value);
        } else if (value instanceof JsonArray) {
            return (JsonArray)value;
        }
        return null;
    }

}

