package org.qubership.itool.di;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

import org.qubership.itool.context.FlowContext;
import org.qubership.itool.context.FlowContextImpl;

/**
 * CLI-specific module that extends the base module with CLI-specific bindings.
 * Can be extended or overridden by extension applications.
 */
public class CliModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(FlowContext.class).to(FlowContextImpl.class).in(Singleton.class);
    }

} 