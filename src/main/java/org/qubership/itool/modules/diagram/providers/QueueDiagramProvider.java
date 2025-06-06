package org.qubership.itool.modules.diagram.providers;

import org.qubership.itool.modules.diagram.Diagram;
import org.qubership.itool.modules.diagram.DiagramService;
import org.qubership.itool.modules.graph.BasicGraph;

public class QueueDiagramProvider extends AbstractQueueDiagramProvider {
    @Override
    public String getType() {
        return "rabbitMQ";
    }

    @Override
    public String generate(DiagramService diagramService, BasicGraph graph, Diagram diagram) {

        String result = generateGraphSchema(diagram, graph);
        return result;
    }
}
