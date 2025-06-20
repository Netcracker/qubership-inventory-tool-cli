package org.qubership.itool.di;

import com.google.inject.Binding;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;

/**
 * Application context that provides access to application services and configuration.
 * This is the main entry point for accessing dependencies in the application.
 * Each instance of ApplicationContext is independent and should be passed to components that need it.
 */
public class ApplicationContext {
    private static final Logger logger = LoggerFactory.getLogger(ApplicationContext.class);
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
        
        // Log the modules being installed
        logger.info("Creating ApplicationContext with {} modules", modules.length);
        for (int i = 0; i < modules.length; i++) {
            logger.info("Module {}: {}", i, modules[i].getClass().getSimpleName());
        }
        
        this.injector = Guice.createInjector(modules);
        
        // Log successful creation
        logger.info("ApplicationContext created successfully");
        
        // Log all Guice bindings
        logBindings();
    }

    /**
     * Log all Guice bindings in this injector.
     */
    private void logBindings() {
        logger.info("=== Guice Bindings ===");
        try {
            Map<Key<?>, Binding<?>> bindings = injector.getAllBindings();
            logger.info("Total bindings: {}", bindings.size());
            for (Map.Entry<Key<?>, Binding<?>> entry : bindings.entrySet()) {
                String key = entry.getKey().toString();
                String bindingType = entry.getValue().getClass().getSimpleName();
                String source = entry.getValue().getSource().toString();
                logger.info("  {} -> [{}] {}", key, bindingType, source);
            }
        } catch (Exception e) {
            logger.warn("Could not log bindings: {}", e.getMessage());
        }
        logger.info("=== End Guice Bindings ===");
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