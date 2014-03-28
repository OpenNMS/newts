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
        properties.put("cassandra.keyspace", System.getProperty("cassandra.keyspace", "newts"));
        properties.put("cassandra.host", System.getProperty("cassandra.host", "localhost"));
        properties.put("cassandra.port", System.getProperty("cassandra.port", "9042"));
        Names.bindProperties(binder(), properties);

        bind(MetricRegistry.class).toInstance(new MetricRegistry());

    }

}
