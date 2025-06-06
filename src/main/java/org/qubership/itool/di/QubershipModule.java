package org.qubership.itool.di;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.util.Modules;

/**
 * Base module for Qubership inventory tool that can be extended by users.
 * This module combines the core functionality with any user-provided customizations.
 */
public class QubershipModule extends AbstractModule {
    
    private final Module userModule;
    
    /**
     * Creates a new QubershipModule with default core functionality.
     */
    public QubershipModule() {
        this(Modules.EMPTY_MODULE);
    }
    
    /**
     * Creates a new QubershipModule that combines core functionality with user customizations.
     * 
     * @param userModule Module containing user customizations
     */
    public QubershipModule(Module userModule) {
        this.userModule = userModule;
    }
    
    @Override
    protected void configure() {
        // Install core functionality
        install(new CoreModule());
        
        // Install user customizations
        install(userModule);
    }
} 