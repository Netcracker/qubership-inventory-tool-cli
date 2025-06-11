package org.qubership.itool.modules.diagram.providers;


import io.vertx.core.json.JsonObject;
import org.apache.commons.collections4.CollectionUtils;
import org.qubership.itool.modules.diagram.Diagram;
import org.qubership.itool.modules.diagram.DiagramService;
import org.qubership.itool.modules.graph.BasicGraph;
import org.qubership.itool.modules.gremlin2.graph.GraphTraversal;

import java.util.List;

public class ApplicationDiagramProvider extends AbstractDiagramProvider {
    @Override
    public String getType() {
        return "application";
    }

    @Override
    public String generate(DiagramService diagramService, BasicGraph graph, Diagram diagram) {
        String[] applications = new String[0];
        if (diagram.getDefaultDomainLevelEntity() != null) {
            applications = new String[1];
            applications[0] = diagram.getDefaultDomainLevelEntity();
        }
        List<String> componentIds = diagram.getComponentIds();
        if (CollectionUtils.isNotEmpty(componentIds)) {
            applications = componentIds.toArray(new String[componentIds.size()]);
        }
        GraphTraversal<JsonObject, JsonObject> traversal = graph.traversal().V().hasType("application");
        if (applications.length != 0) {
            traversal.hasId(applications);
        }

        List<String> microserviceIds = traversal.out().id().toList();
        if (microserviceIds.isEmpty()) {
            return null;
        }
        diagram.setComponentIds(microserviceIds);
        return diagramService.generate("app-microservice", diagram);
    }
}
