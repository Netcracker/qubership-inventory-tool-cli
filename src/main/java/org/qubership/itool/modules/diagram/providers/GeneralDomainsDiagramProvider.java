package org.qubership.itool.modules.diagram.providers;

import io.vertx.core.json.JsonObject;
import org.qubership.itool.modules.diagram.Diagram;
import org.qubership.itool.modules.diagram.DiagramService;
import org.qubership.itool.modules.graph.BasicGraph;
import org.qubership.itool.modules.gremlin2.graph.GraphTraversal;

import java.util.List;
import java.util.Map;

import static org.qubership.itool.modules.graph.Graph.V_DOMAIN;
import static org.qubership.itool.modules.gremlin2.P.neq;
import static org.qubership.itool.modules.gremlin2.P.within;
import static org.qubership.itool.modules.gremlin2.P.without;
import static org.qubership.itool.modules.gremlin2.graph.__.out;

public class GeneralDomainsDiagramProvider extends AbstractDiagramProvider {

    @Override

    public String getType() {
        return "general-domains";
    }

    @Override
    public String generate(DiagramService diagramService, BasicGraph graph, Diagram diagram) {
        String department = diagram.getDepartment();

        GraphTraversal<JsonObject, JsonObject> traversal;
        if (diagram.getDefaultDomainLevelEntity() != null) {
            traversal = graph.traversal().V(diagram.getDefaultDomainLevelEntity());
        } else {
            traversal = graph.traversal().V().hasType(V_DOMAIN);
        }
        if (department != null) {
            traversal = traversal.has("department", department);
        }
        if (diagram.getExcludeDomains().length != 0) {
            traversal = traversal.hasNotId(diagram.getExcludeDomains());
        }

        @SuppressWarnings("unchecked")
        List<Map<Object, String>> dependencyList = traversal
                .union(
                        out().as("SRC").value("/details/domain").as("srcDomain")
                                .outE().as("E")
                                .inV().as("DEST").value("/details/domain").as("destDomain")
                                .where(neq("srcDomain"))
                        , out().as("DEST").value("/details/domain").as("destDomain")
                                .inE().as("E")
                                .outV().as("SRC").value("/details/domain").as("srcDomain")
                                .where(neq("destDomain"))
                        , out().as("SRC").value("/details/domain").as("srcDomain")
                                .outE("implements","defines").as("E")
                                .inV().out("includes").out().as("DEST").value("/details/domain").dedup().as("destDomain")
                                .where(neq("srcDomain"))
                )
                .select("SRC", "DEST", "E", "srcDomain", "destDomain")
                .has("srcDomain", without(diagram.getExcludeDomains()))
                .has("destDomain", without(diagram.getExcludeDomains()))
                .has("/E/type", within("mandatory", "optional", "startup", "graphql", "defines", "implements"))
                .<String>values("source:/srcDomain", "edge:/E/type", "destination:/destDomain").dedup().toList();

        if (dependencyList.isEmpty()) {
            return null;
        }

        String result = generateGraphSchema(graph, diagram, dependencyList);

        return result;
    }

}