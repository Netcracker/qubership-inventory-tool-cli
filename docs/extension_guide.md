# Guide: Extending the Qubership Inventory Tool CLI

This guide explains how to extend the Qubership Inventory Tool CLI with custom functionality. It covers the project structure, extension points, and best practices for creating extensions.

## Table of Contents
1. [Project Structure](#1-project-structure)
2. [Extension Components](#2-extension-components)
3. [Using Application Context](#3-using-application-context-directly)
4. [Extension Points](#4-extension-points)
5. [Example: Creating a Custom Command](#5-example-creating-a-custom-command)
6. [Resources](#6-resources)
7. [Building and Running the Example Project](#7-building-and-running-the-example-project)

## 1. Project Structure

A minimal extension project (for CLI case) should follow this structure:

```bash
qubership-inventory-tool-extension/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── org/qubership/itool/
│   │   │       └── extension/
│   │   │           ├── ExtensionModule.java         # Dependency injection module
│   │   │           ├── ExtensionCommand.java        # Custom command
│   │   │           ├── ExtensionGraphReport.java    # Custom implementation
│   │   │           └── ExtensionCommandFactory.java # Command factory
│   │   └── resources/
│   │       └── META-INF/
│   │           └── services/
│   │               └── io.vertx.core.spi.VerticleFactory  # Command factory registration
└── pom.xml
```

### Key Directories
- `src/main/java/`: Contains Java source code for your extension
- `src/main/resources/META-INF/services/`: Contains service registrations
- `pom.xml`: Project configuration and dependencies

## 2. Extension Components

### 2.1. Dependency Injection Module

The `ExtensionModule` is the core of your extension. It uses Guice's dependency injection to override or extend the main project's functionality.

```java
public class ExtensionModule extends AbstractModule {
    @Override
    protected void configure() {
        // Override existing bindings with custom implementations
        bind(GraphReport.class).to(ExtensionGraphReport.class);
    }
}
```

### 2.2. Custom Command

Custom commands extend existing commands to add new functionality or override behavior. This is the main entry point for extension. The createModules() method allows to override the original modules.

```java
@Name("custom-command")
@Summary("Custom command implementation")
public class ExtensionCommand extends BaseCommand {
    private final Logger logger = LoggerFactory.getLogger(ExtensionCommand.class);
    
    @Override
    protected Module createOverrideModule(Vertx vertx) {
        return new ExtensionModule();
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }
}
```

### 2.3. Command Factory

Commands must be registered through a command factory. Create a factory class and register it in the service manifest:

```java
public class ExtensionCommandFactory implements CommandFactory {
    @Override
    public Command createCommand() {
        return new ExtensionCommand();
    }
}
```

Register the factory in `src/main/resources/META-INF/services/io.vertx.core.spi.VerticleFactory`:
```java
org.qubership.itool.extension.ExtensionCommandFactory
```

### 2.4. Custom Factories and Implementations

Create custom implementations of interfaces and register them in your extension module.

```java
@Singleton
public class ExtensionReportFactory implements GraphReportFactory {
    @Override
    public GraphReport createGraphReport() {
        return new ExtensionGraphReport();
    }
}
```

## 3. Using Application Context directly

The `ApplicationContext` is a crucial component for managing dependency injection and configuration in your extension. It provides a way to access and manage your custom implementations while maintaining the core functionality.

To make sure to get correct instances of classes make sure to never use `new` keyword. Always use factories and instances of classes provided by ApplicationContext.

### 3.1. Creating Application Context

```java
// Create application context with our extension module
ApplicationContext context = new ApplicationContext(
    Vertx.vertx(),
    config,
    new Module[] {
        Modules.override(new QubershipModule(Vertx.vertx()))
               .with(new ExtensionModule())
    }
);
```

### 3.2. Accessing Components

Once you have the application context, you can access your custom implementations:

```java
// Get report instance from application context
GraphReport report = context.getInstance(GraphReport.class);

// Or get provider for it to create a new instance

Provider<GraphReport> reportProvider = context.getProvider(GraphReport.class);
GraphReport report = reportProvider.get();

// Use the report
JsonObject record = new JsonObject()
    .put("id", "test-1")
    .put("name", "Test Record");
report.addRecord(record);

// Access the data
JsonArray records = report.dumpRecords(true);
```

## 4. Extension Points

### 4.1. Commands
- Extend existing commands to add custom behavior
- Override `createModules()` to include your extension module
- Use `@Name` annotation to specify command name
- Use `@Summary` to provide command description
- Register commands through command factories in the service manifest

### 4.2. Tasks
- Add custom task implementations
- Place in resources directory
- Tasks are discovered automatically
- Follow existing task patterns

### 4.3. Dependency Injection
- Override existing bindings
- Add new bindings
- Use Guice's `Modules.override()` for clean extension
- Keep module configuration clean and focused

## 5. Example: Creating a Custom Command

Here's a complete example of creating a custom command:

1. Create the command implementation:
```java
@Name("custom-command")
@Summary("Custom command implementation")
public class CustomCommand extends BaseCommand {
    @Override
    protected Module[] createModules(Vertx vertx) {
        return new Module[] { 
            Modules.override(new QubershipModule(vertx))
                   .with(new ExtensionModule())
        };
    }
}
```

2. Create the command factory:
```java
public class CustomCommandFactory implements CommandFactory {
    @Override
    public Command createCommand() {
        return new CustomCommand();
    }
}
```

3. Register the factory in `src/main/resources/META-INF/services/io.vertx.core.spi.VerticleFactory`:
```java
org.qubership.itool.extension.CustomCommandFactory
```

## 6. Resources

- [Main Project Documentation](https://github.com/Netcracker/qubership-inventory-tool-cli)
- [Vert.x Documentation](https://vertx.io/docs/)
- [Guice Documentation](https://github.com/google/guice/wiki/GettingStarted)

## 7. Building and Running the Example Project

The example project located in `docs/qubership-inventory-tool-extension` demonstrates how to create a custom extension. Here's how to build and run it:

### Building the Project

To build the example project, navigate to the project directory and run:

```bash
mvn clean install
```

This will create a fat JAR file in the `target` directory.

### Running the Example

After building, you can run the example using the following command from the `target` directory:

```bash
java -jar qubership-inventory-tool-extension-*-fat.jar ci-assembly --appname test --inputDir test-data
```

This command will:
- Execute the custom extension
- Run the `ci-assembly` command
- Use "test" as the application name
- Process data from the `test-data` directory

