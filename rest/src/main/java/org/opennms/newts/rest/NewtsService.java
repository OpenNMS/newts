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


import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.lifecycle.Managed;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

import java.util.EnumSet;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration.Dynamic;

import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.opennms.newts.api.SampleRepository;
import org.opennms.newts.api.search.Searcher;

import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import com.google.inject.Guice;
import com.google.inject.Injector;


public class NewtsService extends Application<NewtsConfig> {

    public static void main(String... args) throws Exception {
        new NewtsService().run(args);
    }

    @Override
    public String getName() {
        return "newts";
    }
    
    @Override
    public void initialize(Bootstrap<NewtsConfig> bootstrap) {
        bootstrap.addCommand(new InitCommand());
        bootstrap.addBundle(new AssetsBundle("/app", "/ui", "index.html"));
    }

    @Override
    public void run(NewtsConfig config, Environment environment) throws Exception {

        configureCors(environment);

        Injector injector = Guice.createInjector(new NewtsGuiceModule(), new CassandraGuiceModule(config));

        MetricRegistry metricRegistry = injector.getInstance(MetricRegistry.class);

        // Create/start a JMX reporter for our MetricRegistry
        final JmxReporter reporter = JmxReporter.forRegistry(metricRegistry).inDomain("newts").build();

        environment.lifecycle().manage(new Managed() {

            @Override
            public void stop() throws Exception {
                reporter.stop();
            }

            @Override
            public void start() throws Exception {
                reporter.start();
            }
        });

        SampleRepository repository = injector.getInstance(SampleRepository.class);

        // Add rest resources
        environment.jersey().register(new MeasurementsResource(repository, config.getReports()));
        environment.jersey().register(new SamplesResource(repository));

        // Add search resource only if search is enabled
        if (config.getSearchConfig().isEnabled()) {
            environment.jersey().register(new SearchResource(injector.getInstance(Searcher.class)));
        }

        // Health checks
        environment.healthChecks().register("repository", new RepositoryHealthCheck(repository));

        // Mapped exceptions
        environment.jersey().register(IllegalArgumentExceptionMapper.class);

    }

    // Copy-pasta from http://jitterted.com/tidbits/2014/09/12/cors-for-dropwizard-0-7-x/
    private void configureCors(Environment environment) {
        Dynamic filter = environment.servlets().addFilter("CORS", CrossOriginFilter.class);
        filter.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*");
        filter.setInitParameter(CrossOriginFilter.ALLOWED_METHODS_PARAM, "GET,PUT,POST,DELETE,OPTIONS");
        filter.setInitParameter(CrossOriginFilter.ALLOWED_ORIGINS_PARAM, "*");
        filter.setInitParameter(CrossOriginFilter.ACCESS_CONTROL_ALLOW_ORIGIN_HEADER, "*");
        filter.setInitParameter("allowedHeaders", "Content-Type,Authorization,X-Requested-With,Content-Length,Accept,Origin");
        filter.setInitParameter("allowCredentials", "true");
      }

}
