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

import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.yammer.dropwizard.Service;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.config.Environment;
import com.yammer.dropwizard.lifecycle.Managed;


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
    public void run(NewtsConfig config, Environment environment) throws Exception {

        environment.addFilter(CrossOriginFilter.class, "/*");

        Injector injector;

        if (config.getIndexingConfig().isEnabled()) {
            injector = Guice.createInjector(new CassandraModule(config), new CassandraResourceIndexModule(config));
        }
        else {
            injector = Guice.createInjector(new CassandraModule(config));
        }

        MetricRegistry metricRegistry = injector.getInstance(MetricRegistry.class);
        SampleRepository repository = injector.getInstance(SampleRepository.class);

        // Create/start a JMX reporter for our MetricRegistry
        final JmxReporter reporter = JmxReporter.forRegistry(metricRegistry).inDomain("newts").build();

        environment.manage(new Managed() {

            @Override
            public void stop() throws Exception {
                reporter.stop();
            }

            @Override
            public void start() throws Exception {
                reporter.start();
            }
        });

        // Add rest resources
        environment.addResource(new MeasurementsResource(repository, config.getReports()));
        environment.addResource(new SamplesResource(repository));

        // Health checks
        environment.addHealthCheck(new RepositoryHealthCheck(repository));

        // Mapped exceptions
        environment.addProvider(IllegalArgumentExceptionMapper.class);

    }

}
