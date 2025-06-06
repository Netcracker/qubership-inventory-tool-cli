package org.qubership.itool.modules.diagram;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class DiagramBuilder {

    private Diagram diagram;

    public DiagramBuilder() {
        this.diagram = new Diagram();
    }

    public Diagram build() {
        return this.diagram;
    }

    public DiagramBuilder department(String department) {
        this.diagram.setDepartment(department.length() == 0 ? null : department);
        return this;
    }

    public DiagramBuilder defaultDomainLevelEntity(String defaultDomainLevelEntity) {
        this.diagram.setDefaultDomainLevelEntity(defaultDomainLevelEntity);
        return this;
    }

    public DiagramBuilder defaultComponent(String defaultComponent) {
        this.diagram.setDefaultComponent(defaultComponent);
        return this;
    }

    public DiagramBuilder componentIds(String ... componentIds) {
        this.diagram.setComponentIds(Arrays.asList(componentIds));
        return this;
    }

    public DiagramBuilder addComponentId(String componentId) {
        this.diagram.getComponentIds().add(componentId);
        return this;
    }

    public DiagramBuilder supportedEdges(String ... supportedEdges) {
        this.diagram.setSupportedEdges(supportedEdges);
        return this;
    }

    public DiagramBuilder excludeDomains(String ... excludeDomains) {
        this.diagram.setExcludeDomains(excludeDomains);
        return this;
    }

    public DiagramBuilder producers(List<Map<String, Object>> producers){
        this.diagram.setProducerList(producers);
        return this;
    }

    public DiagramBuilder consumers(List<Map<String, Object>> consumers){
        this.diagram.setConsumerList(consumers);
        return this;
    }

}
