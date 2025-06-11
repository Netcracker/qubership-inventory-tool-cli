package org.qubership.itool.modules.diagram;

import org.qubership.itool.modules.diagram.providers.DiagramProvider;
import org.qubership.itool.modules.graph.Graph;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class DiagramServiceImpl implements DiagramService {

    private Graph graph;

    Properties properties;

    private Map<String, DiagramProvider> diagramProviders = new HashMap<>();

    public DiagramServiceImpl(Graph graph, Properties properties) {
        this.graph = graph;
        this.properties = properties;
    }

    @Override
    public void register(DiagramProvider diagramProvider) {
        diagramProvider.setProperties(this.properties);
        this.diagramProviders.put(diagramProvider.getType(), diagramProvider);
    }

    @Override
    public String generate(String type, Diagram diagram) {
        DiagramProvider diagramProvider = this.diagramProviders.get(type);
        if (diagramProvider == null) {
            throw new IllegalStateException("Can't find DiagramProvider: " + type + ". Available: " + this.diagramProviders.keySet());
        }

        return diagramProvider.generate(this, this.graph, diagram);
    }

}
