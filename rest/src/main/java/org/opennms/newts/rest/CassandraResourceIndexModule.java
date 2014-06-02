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

import org.opennms.newts.api.SampleProcessor;
import org.opennms.newts.api.indexing.ResourceIndex;
import org.opennms.newts.indexing.cassandra.CassandraResourceIndex;
import org.opennms.newts.indexing.cassandra.GuavaCachingIndexState;
import org.opennms.newts.indexing.cassandra.IndexState;
import org.opennms.newts.indexing.cassandra.ResourceIndexingSampleProcessor;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;


/**
 * Guice configuration for Cassandra-based resource index persistence.
 * 
 * @author eevans
 */
public class CassandraResourceIndexModule extends AbstractModule {

    private final NewtsConfig m_newtsConfig;

    public CassandraResourceIndexModule(NewtsConfig newtsConfig) {
        m_newtsConfig = checkNotNull(newtsConfig, "newtsConfig argument");
    }

    @Override
    protected void configure() {

        IndexingConfig conf = m_newtsConfig.getIndexingConfig();

        bind(String.class).annotatedWith(named("index.cassandra.keyspace")).toInstance(conf.getCassandraKeyspace());
        bind(String.class).annotatedWith(named("index.cassandra.host")).toInstance(conf.getCassandraHost());
        bind(Integer.class).annotatedWith(named("index.cassandra.port")).toInstance(conf.getCassandraPort());
        bind(Integer.class).annotatedWith(named("index.cassandra.ttl")).toInstance(conf.getCassandraColumnTTL());
        bind(Integer.class).annotatedWith(named("indexState.maxSize")).toInstance(conf.getMaxCacheEntries());

        bind(IndexState.class).to(GuavaCachingIndexState.class);
        bind(ResourceIndex.class).to(CassandraResourceIndex.class);

        Multibinder<SampleProcessor> processors = Multibinder.newSetBinder(binder(), SampleProcessor.class);
        processors.addBinding().to(ResourceIndexingSampleProcessor.class);

    }

}
