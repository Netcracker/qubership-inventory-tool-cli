package org.qubership.itool.modules.diagram;

import org.qubership.itool.modules.diagram.providers.DiagramProvider;

public interface DiagramService {

    void register(DiagramProvider diagramProvider);

    String generate(String type, Diagram diagram);

}
