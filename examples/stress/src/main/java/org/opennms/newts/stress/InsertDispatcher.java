/*
 * Copyright 2014-2021, The OpenNMS Group
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
package org.opennms.newts.stress;


import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;

import org.opennms.newts.api.DefaultSampleProcessorService;
import org.opennms.newts.api.Sample;
import org.opennms.newts.api.SampleProcessor;
import org.opennms.newts.api.SampleProcessorService;
import org.opennms.newts.api.SampleRepository;
import org.opennms.newts.cassandra.CassandraSession;
import org.opennms.newts.cassandra.CassandraSessionImpl;
import org.opennms.newts.cassandra.ContextConfigurations;
import org.opennms.newts.cassandra.search.CassandraIndexer;
import org.opennms.newts.cassandra.search.CassandraIndexerSampleProcessor;
import org.opennms.newts.cassandra.search.CassandraIndexingOptions;
import org.opennms.newts.cassandra.search.EscapableResourceIdSplitter;
import org.opennms.newts.cassandra.search.GuavaResourceMetadataCache;
import org.opennms.newts.cassandra.search.ResourceIdSplitter;
import org.opennms.newts.persistence.cassandra.CassandraSampleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;


/**
 * A Stress {@link Dispatcher} that inserts {@link Sample} batches using the native API.
 *
 * @author eevans
 */
class InsertDispatcher extends Dispatcher {

    private static final Logger LOG = LoggerFactory.getLogger(InsertDispatcher.class);

    private final InsertConfig m_config;
    private final SampleRepository m_repository;
    private final BlockingQueue<Collection<Sample>> m_samplesQueue;

    InsertDispatcher(InsertConfig config) throws InterruptedException {
        super(config);

        m_config = config;

        CassandraSession session = new CassandraSessionImpl(
                config.getCassandraKeyspace(),
                config.getCassandraHost(),
                config.getCassandraPort(),
                null,
                config.getCassandraCompression(),
                config.getCassandraUsername(),
                config.getCassandraPassword(),
                config.getCassandraSsl());

        ContextConfigurations contexts = new ContextConfigurations();
        Set<SampleProcessor> processors = Sets.newHashSet();

        if (m_config.isSearchEnabled()) {
            ResourceIdSplitter resourceIdSplitter = new EscapableResourceIdSplitter();
            GuavaResourceMetadataCache cache = new GuavaResourceMetadataCache(m_config.getNumResources(), m_metricRegistry);
            CassandraIndexingOptions indexingOptions = new CassandraIndexingOptions.Builder()
                    .withHierarchicalIndexing(m_config.isHierarchicalIndexingEnabled())
                    .withMaxBatchSize(m_config.getBatchSize()).build();
            CassandraIndexer cassandraIndexer = new CassandraIndexer(session, Config.CASSANDRA_TTL,
                    cache, m_metricRegistry, indexingOptions, resourceIdSplitter, contexts);
            CassandraIndexerSampleProcessor indexerSampleProcessor = new CassandraIndexerSampleProcessor(cassandraIndexer);
            processors.add(indexerSampleProcessor);
        }

        SampleProcessorService sampleProcessorService = new DefaultSampleProcessorService(m_config.getThreads(), processors);

        m_repository = new CassandraSampleRepository(
                session,
                Config.CASSANDRA_TTL,
                m_metricRegistry,
                sampleProcessorService,
                contexts);

        m_samplesQueue = Queues.newArrayBlockingQueue(config.getThreads() * 10);

    }

    private void createThreads() {
        for (int i = 0; i < m_config.getThreads(); i++) {
            m_threads[i] = new Inserter(i, m_repository, m_samplesQueue);
        }
    }

    private SampleGenerator[] getSampleGenerators() {
        SampleGenerator[] generators = new SampleGenerator[m_config.getNumResources() * m_config.getNumMetrics()];

        for (int i = 0, pos = 0; i < m_config.getNumResources(); i++) {
            for (int j = 0; j < m_config.getNumMetrics(); j++) {
                generators[pos++] = new SampleGenerator(
                        Joiner.on(ResourceIdSplitter.SEPARATOR).join("class", "domain", "id", "type", "r" + i),
                        "m" + j,
                        m_config.getStart(),
                        m_config.getEnd(),
                        m_config.getInterval());
            }
        }

        return generators;
    }

    @Override
    void go() throws InterruptedException {

        createThreads();
        SampleGenerator[] generators = getSampleGenerators();

        List<Sample> samples = Lists.newArrayList();
        boolean isExhausted = false;
        Meter meter = m_metricRegistry.meter(MetricRegistry.name(getClass(), "samples"));

        outer: while (true) {
            for (int i = 0; i < generators.length; i++) {
                if (generators[i].hasNext()) {
                    Optional<Sample> s = generators[i].next();
                    if (s.isPresent()) {
                        samples.add(s.get());
                        meter.mark();
                    }
                }
                else {
                    // All the iterators yield the same number, when one is exhausted, all are.
                    isExhausted = true;
                }

                // Final queue insertion before shutdown
                if (isExhausted) {
                    LOG.debug("Queuing {} samples for insert", samples.size());
                    m_samplesQueue.put(samples);
                    break outer;
                }

                if (samples.size() >= m_config.getBatchSize()) {
                    LOG.debug("Queuing {} samples for insert", samples.size());
                    m_samplesQueue.put(samples);
                    samples = Lists.newArrayList();
                }

            }
        }

        shutdown();

        LOG.debug("Done.");
    }

}
