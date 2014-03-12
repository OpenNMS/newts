package org.opennms.newts.rest;


import org.opennms.newts.api.SampleRepository;
import org.opennms.newts.persistence.cassandra.CassandraSampleRepository;

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
    }

    @Override
    public void run(Config configuration, Environment environment) throws Exception {

        String host = configuration.getCassandraHost();
        int port = configuration.getCassandraPort();
        String keyspace = configuration.getCassandraKeyspace();

        SampleRepository repository = new CassandraSampleRepository(keyspace, host, port, null);

        environment.addResource(new MeasurementsResource(repository));
        environment.addResource(new SamplesResource(repository));

        environment.addHealthCheck(new RepositoryHealthCheck(repository));

    }

}
