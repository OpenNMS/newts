package org.opennms.newts.gsod;


import java.util.Properties;

import org.opennms.newts.api.SampleRepository;
import org.opennms.newts.persistence.cassandra.CassandraSampleRepository;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.AbstractModule;
import com.google.inject.name.Names;


public class Config extends AbstractModule {

    @Override
    protected void configure() {

        bind(SampleRepository.class).to(CassandraSampleRepository.class);

        Properties properties = new Properties();
        properties.put("cassandraKeyspace", System.getProperty("cassandraKeyspace", "newts"));
        properties.put("cassandraHost", System.getProperty("cassandraHost", "localhost"));
        properties.put("cassandraPort", System.getProperty("cassandraPort", "9042"));
        Names.bindProperties(binder(), properties);

        bind(MetricRegistry.class).toInstance(new MetricRegistry());

    }

}
