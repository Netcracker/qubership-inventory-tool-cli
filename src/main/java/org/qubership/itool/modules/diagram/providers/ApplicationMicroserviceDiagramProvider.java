package org.qubership.itool.modules.diagram.providers;

import io.vertx.core.json.JsonObject;
import org.qubership.itool.modules.diagram.Diagram;
import org.qubership.itool.modules.diagram.DiagramService;
import org.qubership.itool.modules.graph.BasicGraph;
import org.qubership.itool.modules.gremlin2.graph.GraphTraversal;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.qubership.itool.modules.graph.Graph.F_ID;
import static org.qubership.itool.modules.graph.Graph.F_NAME;
import static org.qubership.itool.modules.graph.Graph.F_TYPE;
import static org.qubership.itool.modules.graph.Graph.V_APPLICATION;
import static org.qubership.itool.modules.gremlin2.graph.__.inE;
import static org.qubership.itool.modules.gremlin2.graph.__.outE;
import static org.qubership.itool.modules.gremlin2.graph.__.outV;

public class ApplicationMicroserviceDiagramProvider extends AbstractDiagramProvider {

    @Override
    public String getType() {
        return "app-microservice";
    }

    @Override
    public String generate(DiagramService diagramService, BasicGraph graph, Diagram diagram) {

        String[] componentIds = (diagram.getComponentIds() == null) ? new String[0] : diagram.getComponentIds().toArray(new String[]{});

        GraphTraversal<JsonObject, JsonObject> traversal = graph.traversal().V().hasType(V_APPLICATION);
        traversal.out();
        if (componentIds.length != 0) {
            traversal.hasId(componentIds);
        }
        @SuppressWarnings("unchecked")
        BasicGraph subgraph = traversal.union(
                        inE(diagram.getSupportedEdges()).not(outV().hasType(V_APPLICATION))
                                .subgraph("G")
                        ,
                        outE(diagram.getSupportedEdges()).as("outE").subgraph("G")
                )
                .<BasicGraph>cap("G").next();
        if (subgraph == null) {
            return null;
        }

        return generateApplicationGraphSchema(graph, diagram, subgraph);
    }

    protected String generateApplicationGraphSchema(BasicGraph graph, Diagram diagram, BasicGraph subgraph) {
        if (subgraph == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        Set<String> entityList = new HashSet<>();
        for (JsonObject vertex : subgraph.vertexList()) {
            List<JsonObject> fetchedVertices = fetchVertex(graph, vertex);

            for (JsonObject fetchedVertex : fetchedVertices) {
                if (fetchedVertex == null) {
                    continue;
                }
                String id = fetchedVertex.getString(F_ID);
                String application = graph.traversal().V(id).in().hasType(V_APPLICATION).id().next();
                application = (application == null) ? fetchedVertex.getString(F_TYPE) : application;
                entityList.add(id);
                entityList.add(application);
            }
        }
        buildHeader(builder, diagram, entityList);

        // Elements (Vertex)
        for (JsonObject vertex : subgraph.vertexList()) {
            List<JsonObject> fetchedVertices = fetchVertex(graph, vertex);

            for (JsonObject fetchedVertex : fetchedVertices) {
                if (fetchedVertex == null) {
                    continue;
                }

                String id = fetchedVertex.getString(F_ID);
                String type = fetchedVertex.getString(F_TYPE);
                String name = fetchedVertex.getString(F_NAME);
                String application = graph.traversal().V(id).in().hasType(V_APPLICATION).id().next();
                application = (application == null) ? fetchedVertex.getString(F_TYPE) : application;

                builder.append(getComponentType(diagram, type));
                builder.append(" \"").append(name).append("\" ");
                if ((diagram.getDefaultComponent() == null && application != null && !application.equals(diagram.getDefaultDomainLevelEntity())) ||
                        (diagram.getDefaultComponent() != null && diagram.getDefaultDomainLevelEntity() != null && !id.equals(diagram.getDefaultComponent()))) {
                    builder.append("<<").append(application).append(">> ");
                }
                builder.append(" as ").append(escape(id)).append("\n");
            }
        }
        builder.append("\n");

        // Relations (Edges)
        for (JsonObject edge : subgraph.edgeList()) {
            String edgeId = edge.getString(F_ID);
            String edgeType = edge.getString(F_TYPE);
            List<Map<String, String>> sourceTargetPairs = getOptimizedSourceTarget(subgraph, graph, edgeId);

            for (Map<String, String> pair : sourceTargetPairs) {
                String source = pair.get("source");
                String target = pair.get("target");

                if (source == null || target == null) {
                    continue;
                }

                builder.append(escape(source))
                        .append(" ").append(getEdgeConnector(diagram, edgeType)).append(" ")
                        .append(escape(target)).append("\n");
            }
        }
        builder.append("\n");
        builder.append("@enduml\n");

        return removeDublicatesLine(builder);
    }
}
