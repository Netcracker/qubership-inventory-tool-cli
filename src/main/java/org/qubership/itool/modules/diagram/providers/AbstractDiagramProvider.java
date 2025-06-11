package org.qubership.itool.modules.diagram.providers;

import io.vertx.core.json.JsonObject;
import org.qubership.itool.modules.diagram.Diagram;
import org.qubership.itool.modules.graph.BasicGraph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import static org.qubership.itool.modules.graph.Graph.F_ID;
import static org.qubership.itool.modules.graph.Graph.F_TYPE;

public abstract class AbstractDiagramProvider implements DiagramProvider {

    private Properties properties;

    @Override
    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    @Override
    public Properties getProperties() {
        return this.properties;
    }

    protected String getComponentType(Diagram diagram, String type) {
        return diagram.getVertexElement().getOrDefault(type, "rectangle");
    }

    protected String getEdgeConnector(Diagram diagram, String type) {
        return diagram.getEdgeConnectors().getOrDefault(type, "-->");
    }

    protected String generateGraphSchema(BasicGraph graph, Diagram diagram, List<Map<Object, String>> dependencyList) {
        StringBuilder relationBuilder = new StringBuilder();
        StringBuilder builder = new StringBuilder();
        Set<String> entityList = new HashSet<>();
        for (Map<Object, String> row : dependencyList) {
            String sourceId = row.get("source");
            String destinationId = row.get("destination");
            entityList.add(sourceId);
            entityList.add(destinationId);
        }
        buildHeader(builder, diagram, entityList);

        for (Map<Object, String> row : dependencyList) {
            String sourceId = row.get("source");
            JsonObject sourceVertex = graph.getVertex(sourceId);
            String sourceName = sourceVertex == null ? sourceId : sourceVertex.getString("name");
            String destinationId = row.get("destination");
            JsonObject destVertex = graph.getVertex(destinationId);
            String destinationName = destVertex == null ? destinationId : destVertex.getString("name");
            String edgeType = row.get("edge");

            builder.append("rectangle \"").append(sourceName)
                    .append("\" <<").append(sourceId).append(">> as ").append(escape(sourceId)).append("\n");
            builder.append("rectangle \"").append(destinationName)
                    .append("\" <<").append(destinationId).append(">> as ").append(escape(destinationId)).append("\n");
            relationBuilder.append(escape(sourceId))
                    .append(" ").append(getEdgeConnector(diagram, edgeType)).append(" ")
                    .append(escape(destinationId)).append("\n");
        }

        builder.append(relationBuilder);
        builder.append("@enduml\n");

        return removeDublicatesLine(builder);
    }

    protected String removeDublicatesLine(StringBuilder builder) {
        String result = builder.toString();
        result = Arrays.asList(result.split("\n"))
                .stream().distinct().collect(Collectors.joining("\n"));
        return result;
    }

    protected void buildHeader(StringBuilder builder, Diagram diagram, Set<String> entityList) {
        builder.append("@startuml\n\n");
        builder.append("scale 0.8\n");
        builder.append("left to right direction\n");
        builder.append("skinparam defaultTextAlignment center\n\n");

        // Skinparams
        if (diagram.getDefaultDomainLevelEntity() != null) {
            builder.append("skinparam rectangleBackgroundColor ")
                    .append(getProperties().getProperty(SKINPARAM_BACKGROUND_COLOR_DEFAULT_COMPONENT, "Yellow"))
                    .append("\n");
            builder.append("skinparam artifactBackgroundColor ")
                    .append(getProperties().getProperty(SKINPARAM_BACKGROUND_COLOR_DEFAULT_COMPONENT, "Yellow"))
                    .append("\n");
            builder.append("skinparam collectionsBackgroundColor ")
                    .append(getProperties().getProperty(SKINPARAM_BACKGROUND_COLOR_DEFAULT_COMPONENT, "Yellow"))
                    .append("\n");
        }

        for (String entity : entityList) {
            if (diagram.getDefaultDomainLevelEntity() != null && diagram.getDefaultDomainLevelEntity().equals(entity)) {
                builder.append("skinparam rectangleBackgroundColor<<").append(entity).append(">> ")
                        .append(getProperties().getProperty(SKINPARAM_BACKGROUND_COLOR_DEFAULT_DOMAIN, "Gold"))
                        .append("\n");
                builder.append("skinparam artifactBackgroundColor<<").append(entity).append(">> ")
                        .append(getProperties().getProperty(SKINPARAM_BACKGROUND_COLOR_DEFAULT_DOMAIN, "Gold"))
                        .append("\n");
                builder.append("skinparam collectionsBackgroundColor<<").append(entity).append(">> ")
                        .append(getProperties().getProperty(SKINPARAM_BACKGROUND_COLOR_DEFAULT_DOMAIN, "Gold"))
                        .append("\n");
                continue;
            }

            builder.append("skinparam rectangleBackgroundColor<<").append(entity).append(">> ")
                    .append(getProperties().getProperty(SKINPARAM_BACKGROUND_COLOR_C + entity, "AntiqueWhite"))
                    .append("\n");
            builder.append("skinparam artifactBackgroundColor<<").append(entity).append(">> ")
                    .append(getProperties().getProperty(SKINPARAM_BACKGROUND_COLOR_C + entity, "AntiqueWhite"))
                    .append("\n");
            builder.append("skinparam collectionsBackgroundColor<<").append(entity).append(">> ")
                    .append(getProperties().getProperty(SKINPARAM_BACKGROUND_COLOR_C + entity, "AntiqueWhite"))
                    .append("\n");
        }

        builder.append("skinparam databaseBackgroundColor ")
                .append(getProperties().getProperty(SKINPARAM_BACKGROUND_COLOR_DATABASE, "DeepSkyBlue"))
                .append("\n");

        builder.append("skinparam queueBackgroundColor ")
                .append(getProperties().getProperty(SKINPARAM_BACKGROUND_COLOR_QUEUE, "GreenYellow"))
                .append("\n");

        builder.append("skinparam storageBackgroundColor ")
                .append(getProperties().getProperty(SKINPARAM_BACKGROUND_COLOR_CACHING, "Orchid"))
                .append("\n");
    }

    protected String escape(String str) {
        return str.replaceAll("/|\\s|-|&|\\.|:", "_");
    }

    protected List<JsonObject> fetchVertex(BasicGraph graph, JsonObject vertex) {
        String vertexType = vertex.getString("type");

        if ("cip-element".equals(vertexType)) {
            return cipElementQuery(graph, vertex);
        } else if ("cip-chain".equals(vertexType)) {
            return cipChainQuery(graph, vertex);
        } else {
            return Collections.singletonList(vertex);
        }
    }

    protected List<JsonObject> cipElementQuery(BasicGraph graph, JsonObject vertex){
        return  graph.traversal().V(vertex.getString("id")).in()
                .inE("defines", "implements").outV().dedup().toList();
    }

    protected List<JsonObject> cipChainQuery(BasicGraph graph, JsonObject vertex){
        return graph.traversal().V(vertex.getString("id"))
                .outE("includes").inV().has("type", "cip-element").out().dedup().toList();
    }

    protected List<Map<String, String>> getOptimizedSourceTarget(BasicGraph subgraph, BasicGraph graph, String edgeId) {
        List<Map<String, String>> sourceTargetList = new ArrayList<>();

        String source = subgraph.getEdgeSource(edgeId).getString(F_ID);
        String target = subgraph.getEdgeTarget(edgeId).getString(F_ID);

        String sourceType = subgraph.getEdgeSource(edgeId).getString(F_TYPE);
        String targetType = subgraph.getEdgeTarget(edgeId).getString(F_TYPE);

        if ("cip-element".equals(sourceType)) {
            for (JsonObject cipElement : cipElementQuery(graph, subgraph.getEdgeSource(edgeId))){
                Map<String, String> result = new HashMap<>();
                result.put("source", cipElement.getString(F_ID));
                result.put("target", target);
                sourceTargetList.add(result);
            }
        } else if ("cip-chain".equals(targetType)) {
            for (JsonObject serviceVertices : cipChainQuery(graph,subgraph.getEdgeTarget(edgeId))) {
                Map<String, String> result = new HashMap<>();
                result.put("source", source);
                result.put("target", serviceVertices.getString(F_ID));
                sourceTargetList.add(result);
            }
        } else {
            Map<String, String> result = new HashMap<>();
            result.put("source", source);
            result.put("target", target);
            sourceTargetList.add(result);
        }
        return  sourceTargetList;
    }

}
