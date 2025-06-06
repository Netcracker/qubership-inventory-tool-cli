package org.qubership.itool.modules.graph;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Default implementation of GraphFactory that creates GraphImpl instances.
 */
@Singleton
public class DefaultGraphFactory implements GraphFactory {
    
    private final GraphReportFactory reportFactory;
    
    @Inject
    public DefaultGraphFactory(GraphReportFactory reportFactory) {
        this.reportFactory = reportFactory;
    }
    
    @Override
    public Graph createGraph() {
        Graph graph = new GraphImpl();
        graph.setReport(reportFactory.createGraphReport());
        return graph;
    }
} 