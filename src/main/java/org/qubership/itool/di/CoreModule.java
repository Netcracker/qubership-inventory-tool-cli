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
    
    @Override
    protected void configure() {
        // Bind core services as singletons
        /* bind(GraphService.class).to(GraphServiceImpl.class).in(Singleton.class); */
        bind(MergerApi.class).to(GraphMerger.class).in(Singleton.class);
        
        // Bind managers
      /*   bind(GraphManager.class).in(Singleton.class); */
        
        // Bind factories
        bind(GraphFactory.class).to(DefaultGraphFactory.class).in(Singleton.class);
        bind(GraphReportFactory.class).to(DefaultGraphReportFactory.class).in(Singleton.class);
        
        // GraphFetcher is abstract and must be provided by the user
        /* requireBinding(GraphFetcher.class); */
    }

    /**
     * Default GraphMerger implementation.
     */
    @Provides
    @Singleton
    protected GraphMergerFactory provideGraphMergerFactory(Vertx vertx, GraphFactory graphFactory) {
        return new DefaultGraphMergerFactory(vertx, graphFactory);
    }
} 