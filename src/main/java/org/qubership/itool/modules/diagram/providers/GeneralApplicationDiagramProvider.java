package org.qubership.itool.modules.diagram.providers;

import io.vertx.core.json.JsonObject;
import org.qubership.itool.modules.diagram.Diagram;
import org.qubership.itool.modules.diagram.DiagramService;
import org.qubership.itool.modules.graph.BasicGraph;
import org.qubership.itool.modules.gremlin2.graph.GraphTraversal;

import java.util.List;
import java.util.Map;

import static org.qubership.itool.modules.graph.Graph.V_APPLICATION;
import static org.qubership.itool.modules.gremlin2.P.neq;
import static org.qubership.itool.modules.gremlin2.P.within;
import static org.qubership.itool.modules.gremlin2.graph.__.out;

public class GeneralApplicationDiagramProvider extends AbstractDiagramProvider {
    @Override
    public String getType() {
        return "general-applications";
    }

    @Override
    public String generate(DiagramService diagramService, BasicGraph graph, Diagram diagram) {
        GraphTraversal<JsonObject, JsonObject> traversal;
        if (diagram.getDefaultDomainLevelEntity() != null) {
            traversal = graph.traversal().V(diagram.getDefaultDomainLevelEntity());
        } else {
            traversal = graph.traversal().V().hasType(V_APPLICATION);
        }

        @SuppressWarnings("unchecked")
        List<Map<Object, String>> dependencyList = traversal
                .union(
                        out().in().hasType(V_APPLICATION).hasId(diagram.getDefaultDomainLevelEntity()).id().as("srcApp").out()
                                .outE().as("E")
                                .inV().in().hasType(V_APPLICATION).id().as("destApp")
                                .where(neq("srcApp"))
                        , out().in().hasType(V_APPLICATION).hasId(diagram.getDefaultDomainLevelEntity()).id().as("destApp").out()
                                .inE().as("E")
                                .outV().in().hasType(V_APPLICATION).id().as("srcApp")
                                .where(neq("destApp"))
                        , out().in().hasType(V_APPLICATION).hasId(diagram.getDefaultDomainLevelEntity()).id().as("srcApp").out()
                                .outE("implements","defines").as("E")
                                .inV().out("includes").out().in()
                                .hasType(V_APPLICATION).id().as("destApp")
                                .where(neq("srcApp"))
                )
                .select("E", "srcApp", "destApp")
                .has("/E/type", within("mandatory", "optional", "startup", "graphql", "defines", "implements"))
                .<String>values("source:/srcApp", "edge:/E/type", "destination:/destApp").dedup().toList();

        if (dependencyList.isEmpty()) {
            return null;
        }

        String result = generateGraphSchema(graph, diagram, dependencyList);

        return result;
    }

}
