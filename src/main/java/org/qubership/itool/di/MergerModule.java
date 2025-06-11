package org.qubership.itool.di;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.vertx.core.Vertx;
import org.qubership.itool.modules.processor.DefaultGraphMergerFactory;
import org.qubership.itool.modules.processor.GraphMerger;
import org.qubership.itool.modules.processor.GraphMergerFactory;
import org.qubership.itool.modules.processor.MergerApi;
import org.qubership.itool.modules.graph.GraphFactory;

/**
 * Module for merger-related bindings.
 */
public class MergerModule extends AbstractModule {

    /**
     * Provides the GraphMergerFactory instance.
     */
    @Provides
    @Singleton
    public GraphMergerFactory provideGraphMergerFactory(Vertx vertx, GraphFactory graphFactory) {
        return new DefaultGraphMergerFactory(vertx, graphFactory);
    }

    /**
     * Provides the MergerApi implementation using GraphMergerFactory.
     */
    @Provides
    public MergerApi provideMergerApi(GraphMergerFactory mergerFactory) {
        return mergerFactory.createMerger();
    }
} 