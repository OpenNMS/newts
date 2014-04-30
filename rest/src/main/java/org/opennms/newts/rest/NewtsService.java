/*
 * Copyright 2014, The OpenNMS Group
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.opennms.newts.rest;


import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.opennms.newts.api.SampleRepository;
import org.opennms.newts.persistence.cassandra.CassandraSampleRepository;

import com.yammer.dropwizard.Service;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.config.Environment;


public class NewtsService extends Service<NewtsConfig> {

    public static void main(String... args) throws Exception {
        new NewtsService().run(args);
    }

    @Override
    public void initialize(Bootstrap<NewtsConfig> bootstrap) {
        bootstrap.setName("newts");
        bootstrap.addCommand(new InitCommand());
    }

    @Override
    public void run(NewtsConfig configuration, Environment environment) throws Exception {

        environment.addFilter(CrossOriginFilter.class, "/*");

        String host = configuration.getCassandraHost();
        int port = configuration.getCassandraPort();
        String keyspace = configuration.getCassandraKeyspace();

        SampleRepository repository = new CassandraSampleRepository(keyspace, host, port, null);

        environment.addResource(new MeasurementsResource(repository, configuration.getReports()));
        environment.addResource(new SamplesResource(repository));

        environment.addHealthCheck(new RepositoryHealthCheck(repository));

        environment.addProvider(IllegalArgumentExceptionMapper.class);

    }

}
