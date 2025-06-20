package org.qubership.itool.cli.ci.extension;

import com.google.inject.Module;

import io.vertx.core.Vertx;
import io.vertx.core.cli.annotations.Name;
import io.vertx.core.cli.annotations.Summary;

import org.qubership.itool.cli.ci.CiAssemblyCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extended implementation of CI assembly command that uses our custom module.
 */
@Name("ci-assembly")
@Summary("Extended CI flow: assembly with custom processing")
public class ExtensionCiAssemblyCommand extends CiAssemblyCommand {
    Logger logger = LoggerFactory.getLogger(ExtensionCiAssemblyCommand.class);
    
    @Override
    protected Module createOverrideModule(Vertx vertx) {
        return new ExtensionModule();
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }

} 