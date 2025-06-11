package org.qubership.itool.modules.diagram;

import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.Map;

public class Diagram {

    private String department;

    private String defaultDomainLevelEntity;

    private String defaultComponent;

    private List<String> componentIds;

    private String[] excludeDomains = new String[0];

    private List<Map<String, Object>> producerList;

    private List<Map<String, Object>> consumerList;

    private String[] supportedEdges = new String[] {
        "startup", "optional", "mandatory", "database", "producer", "consumer", "graphql", "library", "defines", "implements"
    };

    private Map<String, String> vertexElement = Map.of(
        "database", "database",
        "mq", "queue",
        "caching", "storage",
        "indexation", "database",
        "ui backend", "artifact",
        "ui", "artifact",
        "ui app bundle", "artifact",
        "library", "collections"
    );

    private Map<String, String> edgeConnectors = Map.of(
        "database", "-",
        "startup", "-[#green]-*",
        "mandatory", "-->",
        "optional", ".[#gray].>",
        "graphql", ".[#blue].#",
        "library", "--+",
        "defines", "-[#DarkCyan]->>",
        "implements", "-[#DarkCyan]->>"
    );

    // ===================================================================================

    public String getDefaultDomainLevelEntity() {
        return defaultDomainLevelEntity;
    }

    public void setDefaultDomainLevelEntity(String defaultDomainLevelEntity) {
        this.defaultDomainLevelEntity = defaultDomainLevelEntity;
    }

    public String getDefaultComponent() {
        return defaultComponent;
    }

    public void setDefaultComponent(String defaultComponent) {
        this.defaultComponent = defaultComponent;
    }

    public List<String> getComponentIds() {
        if (this.defaultComponent != null && CollectionUtils.isEmpty(this.componentIds)) {
            return List.of(defaultComponent);
        }
        return componentIds;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public void setComponentIds(List<String> componentIds) {
        this.componentIds = componentIds;
    }

    public String[] getSupportedEdges() {
        return supportedEdges;
    }

    public void setSupportedEdges(String ... supportedEdges) {
        this.supportedEdges = supportedEdges;
    }

    public Map<String, String> getVertexElement() {
        return vertexElement;
    }

    public void setVertexElement(Map<String, String> vertexElement) {
        this.vertexElement = vertexElement;
    }

    public Map<String, String> getEdgeConnectors() {
        return edgeConnectors;
    }

    public void setEdgeConnectors(Map<String, String> edgeConnectors) {
        this.edgeConnectors = edgeConnectors;
    }

    public String[] getExcludeDomains() {
        return excludeDomains;
    }

    public void setExcludeDomains(String[] excludeDomains) {
        this.excludeDomains = excludeDomains;
    }

    public List<Map<String, Object>> getProducerList() {
        return producerList;
    }

    public void setProducerList(List<Map<String, Object>> producerList) {
        this.producerList = producerList;
    }

    public List<Map<String, Object>> getConsumerList() {
        return consumerList;
    }

    public void setConsumerList(List<Map<String, Object>> consumerList) {
        this.consumerList = consumerList;
    }
}
