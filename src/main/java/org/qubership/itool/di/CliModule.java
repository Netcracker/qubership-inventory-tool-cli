package org.qubership.itool.di;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.vertx.core.Vertx;
import org.qubership.itool.context.FlowContext;
import org.qubership.itool.context.FlowContextImpl;
import org.qubership.itool.modules.graph.GraphFactory;
import org.qubership.itool.modules.graph.GraphReportFactory;
import org.qubership.itool.modules.processor.GraphMergerFactory;

/**
 * CLI-specific module that extends the base module with CLI-specific bindings.
 * Can be extended or overridden by extension applications.
 */
public class CliModule extends AbstractModule {

    /**
     * Default FlowContext with CLI-specific implementation.
     */
    @Provides
    @Singleton
    protected FlowContext provideFlowContext(GraphFactory graphFactory, GraphReportFactory graphReportFactory, GraphMergerFactory graphMergerFactory) {
        return new FlowContextImpl(graphFactory, graphReportFactory, graphMergerFactory);
    }
} 