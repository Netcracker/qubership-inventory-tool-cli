package org.qubership.itool.modules.graph;

/**
 * Factory interface for creating Graph instances.
 * This allows for dependency injection and easier testing.
 */
public interface GraphFactory {
    Graph createGraph();
} 