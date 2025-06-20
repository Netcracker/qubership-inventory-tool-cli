package org.qubership.itool.cli.ci.extension;

import org.qubership.itool.modules.report.GraphReportImpl;

import io.vertx.core.json.JsonObject;

/**
 * Extended implementation of GraphReport that adds custom functionality.
 */
public class ExtensionGraphReport extends GraphReportImpl {
    
    public ExtensionGraphReport() {
        super();
    }

    @Override
    public void addRecord(JsonObject record) {
        super.addRecord(record);
        record.put("extended", true);
        LOG.info("[Extended] Adding record: {}", record);
    }
    // Add custom methods here
} 