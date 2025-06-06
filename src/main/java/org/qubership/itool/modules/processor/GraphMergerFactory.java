package org.qubership.itool.modules.processor;

import io.vertx.core.Vertx;
import org.qubership.itool.modules.graph.GraphFactory;
import org.qubership.itool.modules.graph.GraphReportFactory;

/**
 * Factory for creating GraphMerger instances.
 * This allows consumers to create their own instances when needed.
 */
public interface GraphMergerFactory {
    /**
     * Create a new GraphMerger instance.
     * 
     * @return A new GraphMerger instance
     */
    GraphMerger createMerger();
} 