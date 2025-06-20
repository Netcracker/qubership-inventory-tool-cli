package org.qubership.itool.cli.ci.extension;

import com.google.inject.AbstractModule;
import org.qubership.itool.modules.report.GraphReport;

/**
 * Extension module that provides custom implementations of graph factories.
 */
public class ExtensionModule extends AbstractModule {


    @Override
    protected void configure() {
        // Override existing bindings with our custom implementations
        bind(GraphReport.class).to(ExtensionGraphReport.class);
    }


} 