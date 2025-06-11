package org.qubership.itool.modules.diagram.providers;

import io.vertx.core.json.JsonObject;
import org.apache.commons.collections4.CollectionUtils;
import org.qubership.itool.modules.diagram.Diagram;
import org.qubership.itool.modules.diagram.DiagramService;
import org.qubership.itool.modules.graph.BasicGraph;
import org.qubership.itool.modules.gremlin2.graph.GraphTraversal;

import java.util.List;

public class DomainDiagramProvider extends AbstractDiagramProvider {

    @Override
    public String getType() {
        return "domain";
    }

    @Override
    public String generate(DiagramService diagramService, BasicGraph graph, Diagram diagram) {
        String department = diagram.getDepartment();

        String[] excludeDomains = (diagram.getExcludeDomains() == null) ? new String[0] : diagram.getExcludeDomains();
        String[] domains = new String[0];
        if (diagram.getDefaultDomainLevelEntity() != null) {
            domains = new String[1];
            domains[0] = diagram.getDefaultDomainLevelEntity();
        }
        List<String> componentIds = diagram.getComponentIds();
        if (CollectionUtils.isNotEmpty(componentIds)) {
            domains = componentIds.toArray(new String[componentIds.size()]);
        }
        GraphTraversal<JsonObject, JsonObject> traversal = graph.traversal().V().hasType("domain");
        if (department != null) {
            traversal.has("department", department);
        }
        if (excludeDomains.length != 0) {
            traversal.hasNotId(excludeDomains);
        }
        if (domains.length != 0) {
            traversal.hasId(domains);
        }

        List<String> microserviceIds = traversal.out().id().toList();
        if (microserviceIds.isEmpty()) {
            return null;
        }
        diagram.setComponentIds(microserviceIds);
        return diagramService.generate("microservice", diagram);
    }

}