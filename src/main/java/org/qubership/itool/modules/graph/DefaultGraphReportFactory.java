package org.qubership.itool.modules.graph;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.qubership.itool.modules.report.GraphReport;
import org.qubership.itool.modules.report.GraphReportImpl;

/**
 * Default implementation of GraphReportFactory that creates GraphReportImpl instances.
 */
@Singleton
public class DefaultGraphReportFactory implements GraphReportFactory {
    
    @Inject
    public DefaultGraphReportFactory() {
    }
    
    @Override
    public GraphReport createGraphReport() {
        return new GraphReportImpl();
    }
} 