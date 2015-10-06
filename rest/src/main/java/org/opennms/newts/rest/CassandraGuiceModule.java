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
import org.opennms.newts.api.SampleRepository;
import org.opennms.newts.api.search.Indexer;
import org.opennms.newts.api.search.Searcher;
import org.opennms.newts.cassandra.ContextConfigurations;
import org.opennms.newts.cassandra.search.CassandraIndexer;
import org.opennms.newts.cassandra.search.CassandraIndexerSampleProcessor;
import org.opennms.newts.cassandra.search.CassandraSearcher;
import org.opennms.newts.cassandra.search.EscapableResourceIdSplitter;
import org.opennms.newts.cassandra.search.GuavaResourceMetadataCache;
import org.opennms.newts.cassandra.search.ResourceIdSplitter;
import org.opennms.newts.cassandra.search.ResourceMetadataCache;
import org.opennms.newts.cassandra.search.SimpleResourceIdSplitter;
import org.opennms.newts.persistence.cassandra.CassandraSampleRepository;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;


/**
 * Guice configuration for Cassandra sample storage.
 * 
 * @author eevans
 */
public class CassandraGuiceModule extends AbstractModule {

    private final NewtsConfig m_newtsConf;

    public CassandraGuiceModule(NewtsConfig newtsConfig) {
        m_newtsConf = checkNotNull(newtsConfig, "newtsConfig argument");
    }

    @Override
    protected void configure() {

        bind(String.class).annotatedWith(named("cassandra.keyspace")).toInstance(m_newtsConf.getCassandraKeyspace());
        bind(String.class).annotatedWith(named("cassandra.hostname")).toInstance(m_newtsConf.getCassandraHost());
        bind(Integer.class).annotatedWith(named("cassandra.port")).toInstance(m_newtsConf.getCassandraPort());
        bind(String.class).annotatedWith(named("cassandra.compression")).toInstance(m_newtsConf.getCassandraCompression());
        bind(String.class).annotatedWith(named("cassandra.username")).toInstance(m_newtsConf.getCassandraUsername());
        bind(String.class).annotatedWith(named("cassandra.password")).toInstance(m_newtsConf.getCassandraPassword());

        bind(Integer.class).annotatedWith(named("samples.cassandra.time-to-live")).toInstance(m_newtsConf.getCassandraColumnTTL());
        bind(Integer.class).annotatedWith(named("search.cassandra.time-to-live")).toInstance(m_newtsConf.getCassandraColumnTTL());
        bind(Integer.class).annotatedWith(named("sampleProcessor.maxThreads")).toInstance(m_newtsConf.getMaxSampleProcessorThreads());

        bind(Long.class).annotatedWith(named("search.resourceMetadata.maxCacheEntries")).toInstance(m_newtsConf.getSearchConfig().getMaxCacheEntries());
        bind(Boolean.class).annotatedWith(named("search.hierarical-indexing")).toInstance(m_newtsConf.getSearchConfig().isHierarchicalIndexingEnabled());

        bind(ResourceMetadataCache.class).to(GuavaResourceMetadataCache.class);
        bind(Searcher.class).to(CassandraSearcher.class);
        bind(SampleRepository.class).to(CassandraSampleRepository.class);
        bind(Indexer.class).to(CassandraIndexer.class);

        Multibinder<SampleProcessor> processors = Multibinder.newSetBinder(binder(), SampleProcessor.class);

        if (m_newtsConf.getSearchConfig().isSeparatorEscapingEnabled()) {
            bind(ResourceIdSplitter.class).to(EscapableResourceIdSplitter.class);
        } else {
            bind(ResourceIdSplitter.class).to(SimpleResourceIdSplitter.class);
        }

        // Only add the search indexer if search is enabled
        if (m_newtsConf.getSearchConfig().isEnabled()) {
            processors.addBinding().to(CassandraIndexerSampleProcessor.class);
        }

        // Pull in context specific attributes
        ContextConfigurations contextConfigurations = new ContextConfigurations();
        for (ContextConfig contextConfig : m_newtsConf.getContextConfigs().values()) {
            contextConfigurations.addContextConfig(contextConfig.getContext(), contextConfig.getResourceShard(),
                    contextConfig.getReadConsistency(), contextConfig.getWriteConsistency());
        }
        bind(ContextConfigurations.class).toInstance(contextConfigurations);
    }

}
