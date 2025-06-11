package org.qubership.itool.di;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

/**
 * Application context that provides access to application services and configuration.
 * This is the main entry point for accessing dependencies in the application.
 * Each instance of ApplicationContext is independent and should be passed to components that need it.
 */
public class ApplicationContext {
    private final Injector injector;
    private final Vertx vertx;
    private final JsonObject config;

    /**
     * Create a new application context.
     * 
     * @param vertx The Vertx instance to use
     * @param config The application configuration
     * @param modules The modules to use for dependency injection
     */
    public ApplicationContext(Vertx vertx, JsonObject config, Module[] modules) {
        this.vertx = vertx;
        this.config = config;
        this.injector = Guice.createInjector(modules);
    }

    /**
     * Create a new application context with default settings.
     * 
     * @return A new application context
     */
    public static ApplicationContext createDefault() {
        Vertx vertx = Vertx.vertx();
        return new ApplicationContext(vertx, new JsonObject(), new Module[] {new QubershipModule(vertx)});
    }

    /**
     * Get an instance of the specified type from the injector.
     * 
     * @param type The type to get an instance of
     * @return An instance of the specified type
     */
    public <T> T getInstance(Class<T> type) {
        return injector.getInstance(type);
    }

    /**
     * Get the Vertx instance associated with this context.
     * 
     * @return The Vertx instance
     */
    public Vertx getVertx() {
        return vertx;
    }

    /**
     * Get the application configuration.
     * 
     * @return The application configuration
     */
    public JsonObject getConfig() {
        return config;
    }

    /**
     * Get the injector instance.
     * This should be used sparingly and only when direct access to the injector is necessary.
     * 
     * @return The injector instance
     */
    public Injector getInjector() {
        return injector;
    }
} 