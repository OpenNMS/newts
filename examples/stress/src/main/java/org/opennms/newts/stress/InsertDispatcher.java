package org.opennms.newts.stress;


import java.util.List;

import org.opennms.newts.api.Sample;
import org.opennms.newts.api.SampleProcessorService;
import org.opennms.newts.api.SampleRepository;
import org.opennms.newts.persistence.cassandra.CassandraSampleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;


/**
 * A Stress {@link Dispatcher} that inserts {@link Sample} batches using the native API.
 *
 * @author eevans
 */
class InsertDispatcher extends Dispatcher {

    /** Number of seconds to keep Cassandra-stored samples. */
    static int CASSANDRA_TTL = 86400;

    private static final Logger LOG = LoggerFactory.getLogger(InsertDispatcher.class);

    private final SampleRepository m_repository;

    InsertDispatcher(Config config) throws InterruptedException {
        super(config);

        m_repository = new CassandraSampleRepository(
                config.getCassandraKeyspace(),
                config.getCassandraHost(),
                config.getCassandraPort(),
                CASSANDRA_TTL,
                new MetricRegistry(),
                new SampleProcessorService(1));
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
                        "r" + i,
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

        outer: while (true) {

            inner: for (int i = 0; i < generators.length; i++) {
                if (generators[i].hasNext()) {
                    Optional<Sample> s = generators[i].next();
                    if (s.isPresent()) samples.add(s.get());
                }
                else {
                    // All the iterators yield the same number, when one is exhausted, all are.
                    isExhausted = true;
                    break inner;
                }
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

        shutdown();

        LOG.debug("Done.");
    }

}
