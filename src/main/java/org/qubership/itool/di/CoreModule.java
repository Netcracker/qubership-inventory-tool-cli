package org.qubership.itool.di;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import io.vertx.core.Vertx;

import org.qubership.itool.modules.processor.DefaultGraphMergerFactory;
import org.qubership.itool.modules.processor.GraphMerger;
import org.qubership.itool.modules.processor.GraphMergerFactory;
import org.qubership.itool.modules.processor.MergerApi;
import org.qubership.itool.modules.graph.GraphReportFactory;
import org.qubership.itool.modules.graph.DefaultGraphReportFactory;
import org.qubership.itool.modules.graph.DefaultGraphFactory;
import org.qubership.itool.modules.graph.GraphFactory;

/**
 * Core module that provides basic bindings for the core functionality.
 * This module can be extended or overridden by users of the library.
 */
public class CoreModule extends AbstractModule {

    private final Vertx vertx;
    
    public CoreModule(Vertx vertx) {
        this.vertx = vertx;
    }

    @Override
    protected void configure() {
        // Bind core factories
        bind(GraphFactory.class).to(DefaultGraphFactory.class).in(Singleton.class);
        bind(GraphReportFactory.class).to(DefaultGraphReportFactory.class).in(Singleton.class);
    }

    /**
     * Provides the Vertx instance for the application.
     */
    @Provides
    @Singleton
    public Vertx provideVertx() {
        return vertx;
    }
} 