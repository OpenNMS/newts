package org.opennms.newts.cli;


import com.codahale.metrics.MetricRegistry;
import com.google.inject.AbstractModule;


public class StandardGuiceModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(MetricRegistry.class).toInstance(new MetricRegistry());
    }

}
