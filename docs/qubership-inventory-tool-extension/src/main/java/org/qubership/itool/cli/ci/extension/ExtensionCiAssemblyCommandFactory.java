package org.qubership.itool.cli.ci.extension;

import io.vertx.core.spi.launcher.DefaultCommandFactory;

/**
 * Factory for creating ExtensionCiAssemblyCommand instances.
 */
public class ExtensionCiAssemblyCommandFactory extends DefaultCommandFactory<ExtensionCiAssemblyCommand> {

    public ExtensionCiAssemblyCommandFactory() {
        super(ExtensionCiAssemblyCommand.class, ExtensionCiAssemblyCommand::new);
    }
} 