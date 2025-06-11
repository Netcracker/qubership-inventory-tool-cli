package org.qubership.itool.modules.processor;

import io.vertx.core.Vertx;
import org.qubership.itool.modules.graph.GraphFactory;
import com.google.inject.Inject;


/**
 * Default implementation of GraphMergerFactory.
 */
public class DefaultGraphMergerFactory implements GraphMergerFactory {
    private final Vertx vertx;
    private final GraphFactory graphFactory;

    @Inject
    public DefaultGraphMergerFactory(Vertx vertx, GraphFactory graphFactory) {
        this.vertx = vertx;
        this.graphFactory = graphFactory;
    }

    @Override
    public GraphMerger createMerger() {
        return new GraphMerger(vertx, graphFactory);
    }
} 