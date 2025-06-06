package org.qubership.itool.modules.diagram.providers;


import io.vertx.core.json.JsonObject;
import org.qubership.itool.modules.diagram.Diagram;
import org.qubership.itool.modules.diagram.DiagramService;
import org.qubership.itool.modules.graph.BasicGraph;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class InfrastructureDiagramProvider extends AbstractDiagramProvider {

    @Override
    public String getType() {
        return "infrastructure";
    }

    @Override
    public String generate(DiagramService diagramService, BasicGraph graph, Diagram diagram) {
        String[] entityIds = diagram.getComponentIds().toArray(new String[]{});

        List<Map<String, JsonObject>> jsonList = graph.traversal().V("Infra")
            .out().hasId(entityIds).as("infraComponent")
            .outE().as("outEdge")
            .inV().as("component")
            .<JsonObject>select("infraComponent", "outEdge", "component").toList();

        Set<String> microservicesSet = new HashSet<>();
        Set<String> edgesSet = new HashSet<>();

        StringBuilder result = new StringBuilder();
        // Outgoing relations
        for (Map<String, JsonObject> map : jsonList) {
            JsonObject infraComponent = map.get("infraComponent");
            JsonObject outEdge = map.get("outEdge");
            JsonObject component = map.get("component");
            microservicesSet.add(component(null, infraComponent));
            microservicesSet.add(component(null, component));

            String relation = relation(
                infraComponent.getString("id"),
                component.getString("id"),
                outEdge.getString("type"),
                outEdge.getString("name"),
                false
            );
//            System.out.println(outEdge.getString("name"));
            edgesSet.add(relation);
        }

        // Incoming relations
        jsonList = graph.traversal().V("Infra")
            .out().hasId(entityIds).as("infraComponent").inE().as("inEdge")
            .outV().hasNotId("Infra").as("component")
            .<JsonObject>select("infraComponent", "inEdge", "component").toList();
        for (Map<String, JsonObject> map : jsonList) {
            JsonObject infraComponent = map.get("infraComponent");
            JsonObject inEdge = map.get("inEdge");
            JsonObject component = map.get("component");
            microservicesSet.add(component(null, infraComponent));
            microservicesSet.add(component(null, component));

            String relation = relation(
                component.getString("id"),
                infraComponent.getString("id"),
                inEdge.getString("type"),
                inEdge.getString("name"),
                false
            );
//            System.out.println(inEdge.getString("name"));
            edgesSet.add(relation);
        }

        for (String m : microservicesSet) {
            result.append(m).append("\n");
        }
        result.append("\n");
        for (String m : edgesSet) {
            result.append(m).append("\n");
        }

        String r = result.toString();
        r = Arrays.asList(r.split("\n"))
            .stream().distinct().collect(Collectors.joining("\n"));

        return header(graph) + r + footer(graph);
    }

}
