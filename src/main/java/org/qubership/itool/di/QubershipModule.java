package org.qubership.itool.di;

import com.google.inject.AbstractModule;
import io.vertx.core.Vertx;

/**
 * Base module for Qubership inventory tool that can be extended by users.
 * This module combines the core functionality with any user-provided customizations.
 */
public class QubershipModule extends AbstractModule {

    private final Vertx vertx;
    
    /**
     * Creates a new QubershipModule with default core functionality.
     */
    public QubershipModule(Vertx vertx) {
        this.vertx = vertx;
    }
    
    @Override
    protected void configure() {
        // Install core functionality
        install(new CoreModule(vertx));

        // Install merger functionality
        install(new MergerModule());
        
        // Install CLI functionality
        install(new CliModule());
    }
 
} 