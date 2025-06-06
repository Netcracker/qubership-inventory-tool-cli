package org.qubership.itool.modules.graph;

import org.qubership.itool.modules.report.GraphReport;

/**
 * Factory interface for creating GraphReport instances.
 * This allows for dependency injection and easier testing.
 */
public interface GraphReportFactory {
    GraphReport createGraphReport();
} 