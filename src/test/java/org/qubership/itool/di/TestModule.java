package org.qubership.itool.di;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.util.Modules;
import io.vertx.core.Vertx;
import org.qubership.itool.context.FlowContext;
import org.qubership.itool.modules.graph.GraphReportFactory;
import org.qubership.itool.modules.processor.GraphMerger;

/**
 * Test module that demonstrates how to override core services with test implementations.
 * Uses Guice's override feature to replace existing bindings.
 */
public class TestModule extends AbstractModule {
    private final GraphMerger mockGraphMerger;
    private final FlowContext mockFlowContext;
    private final GraphReportFactory customGraphReportFactory;

    public TestModule(GraphMerger mockGraphMerger,
                     FlowContext mockFlowContext,
                     GraphReportFactory customGraphReportFactory) {
        this.mockGraphMerger = mockGraphMerger;
        this.mockFlowContext = mockFlowContext;
        this.customGraphReportFactory = customGraphReportFactory;
    }

    @Override
    protected void configure() {
        // Override existing bindings with mocks and custom implementations
        bind(GraphMerger.class).toInstance(mockGraphMerger);
        bind(FlowContext.class).toInstance(mockFlowContext);
        bind(GraphReportFactory.class).toInstance(customGraphReportFactory);
    }

    /**
     * Create a module that overrides the base CLI module with test implementations.
     * 
     * @param vertx The Vertx instance
     * @param mockGraphMerger The mock GraphMerger
     * @param mockFlowContext The mock FlowContext
     * @param customGraphReportFactory Custom GraphReportFactory implementation
     * @return A module that can be used with ApplicationContext
     */
    public static Module createOverrideModule(Vertx vertx, 
                                            GraphMerger mockGraphMerger,
                                            FlowContext mockFlowContext,
                                            GraphReportFactory customGraphReportFactory) {
        return Modules.override(new CliModule(vertx))
                     .with(new TestModule(mockGraphMerger, mockFlowContext, customGraphReportFactory));
    }
} 