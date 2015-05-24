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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.inject.name.Names.named;

import com.google.inject.AbstractModule;


/**
 * Graphite Guice configuration.
 * 
 * @author eevans
 */
public class GraphiteGuiceModule extends AbstractModule {

    private final GraphiteConfig m_config;
    
    public GraphiteGuiceModule(NewtsConfig config) {
        checkNotNull(config, "config argument");
        m_config = config.getGraphiteConfig();
    }
    
    @Override
    protected void configure() {
        bind(Integer.class).annotatedWith(named("graphite.port")).toInstance(m_config.getPort());
    }

}
