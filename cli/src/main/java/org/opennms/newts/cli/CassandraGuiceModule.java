package org.opennms.newts.cli;


import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.inject.name.Names.named;

import org.opennms.newts.api.SampleProcessor;
import org.opennms.newts.api.SampleRepository;
import org.opennms.newts.api.search.Searcher;
import org.opennms.newts.cassandra.search.CassandraIndexerSampleProcessor;
import org.opennms.newts.cassandra.search.CassandraSearcher;
import org.opennms.newts.cassandra.search.GuavaResourceMetadataCache;
import org.opennms.newts.cassandra.search.ResourceMetadataCache;
import org.opennms.newts.persistence.cassandra.CassandraSampleRepository;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;


public class CassandraGuiceModule extends AbstractModule {

    private final Config m_config;

    public CassandraGuiceModule(Config config) {
        m_config = checkNotNull(config, "config argument");
    }

    @Override
    protected void configure() {

        bind(String.class).annotatedWith(named("cassandra.keyspace")).toInstance(m_config.getCassandraKeyspace());
        bind(String.class).annotatedWith(named("cassandra.hostname")).toInstance(m_config.getCassandraHostname());
        bind(Integer.class).annotatedWith(named("cassandra.port")).toInstance(m_config.getCassandraPort());
        bind(Integer.class).annotatedWith(named("samples.cassandra.time-to-live")).toInstance(m_config.getCassandraSamplesTTL());
        bind(Integer.class).annotatedWith(named("search.cassandra.time-to-live")).toInstance(m_config.getCassandraSearchTTL());

        // XXX: Achtung; Hard-coding ahead!
        bind(Integer.class).annotatedWith(named("sampleProcessor.maxThreads")).toInstance(32);        
        bind(Long.class).annotatedWith(named("search.rMetadata.maxCacheSize")).toInstance(1000000L);

        bind(ResourceMetadataCache.class).to(GuavaResourceMetadataCache.class);
        bind(Searcher.class).to(CassandraSearcher.class);
        bind(SampleRepository.class).to(CassandraSampleRepository.class);

        Multibinder<SampleProcessor> processors = Multibinder.newSetBinder(binder(), SampleProcessor.class);

        // Only add the search indexer if search is enabled
        if (m_config.isSearchEnabled()) {
            processors.addBinding().to(CassandraIndexerSampleProcessor.class);
        }

    }

}
