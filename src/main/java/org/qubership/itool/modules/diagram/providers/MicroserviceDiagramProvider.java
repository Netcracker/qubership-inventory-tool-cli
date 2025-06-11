package org.qubership.itool.modules.diagram.providers;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;
import org.qubership.itool.modules.diagram.Diagram;
import org.qubership.itool.modules.diagram.DiagramService;
import org.qubership.itool.modules.graph.BasicGraph;
import org.qubership.itool.modules.gremlin2.P;
import org.qubership.itool.modules.gremlin2.graph.GraphTraversal;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.qubership.itool.modules.graph.Graph.F_ID;
import static org.qubership.itool.modules.graph.Graph.F_NAME;
import static org.qubership.itool.modules.graph.Graph.F_TYPE;
import static org.qubership.itool.modules.graph.Graph.V_DOMAIN;
import static org.qubership.itool.modules.gremlin2.graph.__.inE;
import static org.qubership.itool.modules.gremlin2.graph.__.inV;
import static org.qubership.itool.modules.gremlin2.graph.__.outE;
import static org.qubership.itool.modules.gremlin2.graph.__.outV;

public class MicroserviceDiagramProvider extends AbstractDiagramProvider {

    @Override
    public String getType() {
        return "microservice";
    }

    @Override
    public String generate(DiagramService diagramService, BasicGraph graph, Diagram diagram) {
        String department = diagram.getDepartment();

        String[] componentIds = (diagram.getComponentIds() == null) ? new String[0] : diagram.getComponentIds().toArray(new String[]{});

        String[] excludeDomains = (diagram.getExcludeDomains() == null) ? new String[0] : diagram.getExcludeDomains();

        GraphTraversal<JsonObject, JsonObject> traversal = graph.traversal().V();
        if (excludeDomains.length != 0) {
            traversal.hasType(V_DOMAIN).hasNotId(excludeDomains);
        }
        if (department != null) {
            traversal.hasType(V_DOMAIN).has("department", department);
        }
        traversal.out();
        if (componentIds.length != 0) {
            traversal.hasId(componentIds);
        }
        @SuppressWarnings("unchecked")
        BasicGraph subgraph = traversal.union(
                        inE(diagram.getSupportedEdges()).not(outV().hasType("domain"))
                                .not(outV().has("/details/domain", P.within(excludeDomains))).subgraph("G")
                        ,
                        outE(diagram.getSupportedEdges()).as("outE")
                                .not(inV().has("/details/domain", P.within(excludeDomains))).subgraph("G")
                )
                .<BasicGraph>cap("G").next();
        if (subgraph == null) {
            return null;
        }
        String result = generateDomainGraphSchema(graph, diagram, subgraph);

        return result;
    }

    protected String generateDomainGraphSchema(BasicGraph graph, Diagram diagram, BasicGraph subgraph) {
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
                String domain = (String) JsonPointer.from("/details/domain").queryJson(fetchedVertex);
                domain = (domain == null) ? fetchedVertex.getString(F_TYPE) : domain;
                entityList.add(id);
                entityList.add(domain);
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
                String domain = (String) JsonPointer.from("/details/domain").queryJson(fetchedVertex);
                domain = (domain == null) ? fetchedVertex.getString(F_TYPE) : domain;

                builder.append(getComponentType(diagram, type));
                builder.append(" \"").append(name).append("\" ");
                if ((diagram.getDefaultComponent() == null && domain != null && !domain.equals(diagram.getDefaultDomainLevelEntity())) ||
                        (diagram.getDefaultComponent() != null && diagram.getDefaultDomainLevelEntity() != null && !id.equals(diagram.getDefaultComponent()))) {
                    builder.append("<<").append(domain).append(">> ");
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

        String result = removeDublicatesLine(builder);
        return result;
    }

}