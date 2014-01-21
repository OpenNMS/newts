package org.opennms.newts.gsod;


import org.opennms.newts.api.MeasurementRepository;
import org.opennms.newts.persistence.cassandra.CassandraMeasurementRepository;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.AbstractModule;
import com.google.inject.name.Names;


public class Config extends AbstractModule {

    @Override
    protected void configure() {

        bind(MeasurementRepository.class).to(CassandraMeasurementRepository.class);

        bind(String.class).annotatedWith(Names.named("cassandraKeyspace")).toInstance("newts");
        bind(String.class).annotatedWith(Names.named("cassandraHost")).toInstance("localhost");
        bind(Integer.class).annotatedWith(Names.named("cassandraPort")).toInstance(9042);

        bind(MetricRegistry.class).toInstance(new MetricRegistry());

    }

}
