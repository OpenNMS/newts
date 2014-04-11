package org.opennms.newts.rest;


import java.io.File;

import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.opennms.newts.api.SampleRepository;
import org.opennms.newts.persistence.cassandra.CassandraSampleRepository;
import org.opennms.newts.persistence.leveldb.LeveldbSampleRepository;

import com.codahale.metrics.MetricRegistry;
import com.yammer.dropwizard.Service;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.config.Environment;


public class NewtsService extends Service<Config> {

    public static void main(String... args) throws Exception {
        new NewtsService().run(args);
    }

    @Override
    public void initialize(Bootstrap<Config> bootstrap) {
        bootstrap.setName("newts");
        bootstrap.addCommand(new InitCommand());
    }

    @Override
    public void run(Config configuration, Environment environment) throws Exception {

	MetricRegistry registry = new MetricRegistry();

        environment.addFilter(CrossOriginFilter.class, "/*");

        String host = configuration.getCassandraHost();
        int port = configuration.getCassandraPort();
        String keyspace = configuration.getCassandraKeyspace();
        File databaseDir = configuration.getLeveldbDir();

        //SampleRepository repository = new CassandraSampleRepository(keyspace, host, port, registry);
        SampleRepository repository = new LeveldbSampleRepository(databaseDir, registry);

        environment.addResource(new MeasurementsResource(repository, configuration.getReports()));
        environment.addResource(new SamplesResource(repository));

        environment.addHealthCheck(new RepositoryHealthCheck(repository));

        environment.addProvider(IllegalArgumentExceptionMapper.class);

    }

}
