package org.opennms.newts.stress;


import java.util.concurrent.BlockingQueue;

import org.opennms.newts.aggregate.IntervalGenerator;
import org.opennms.newts.api.Sample;
import org.opennms.newts.api.SampleProcessorService;
import org.opennms.newts.api.SampleRepository;
import org.opennms.newts.api.Timestamp;
import org.opennms.newts.api.query.ResultDescriptor;
import org.opennms.newts.api.query.StandardAggregationFunctions;
import org.opennms.newts.cassandra.CassandraSession;
import org.opennms.newts.persistence.cassandra.CassandraSampleRepository;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.Queues;


/**
 * A Stress {@link Dispatcher} that selects {@link Sample samples} using the native API.
 *
 * @author eevans
 */
public class SelectDispatcher extends Dispatcher {

    private final SelectConfig m_config;
    private final SampleRepository m_repository;
    private final BlockingQueue<Query> m_queryQueue;

    SelectDispatcher(SelectConfig config) {
        super(config);

        m_config = config;

        CassandraSession session = new CassandraSession(
                config.getCassandraKeyspace(),
                config.getCassandraHost(),
                config.getCassandraPort(),
                config.getCassandraCompression());
        m_repository = new CassandraSampleRepository(
                session,
                Config.CASSANDRA_TTL,
                new MetricRegistry(),
                new SampleProcessorService(1));

        m_queryQueue = Queues.newArrayBlockingQueue(config.getThreads() * 10);

    }

    private ResultDescriptor getResultDescriptor() {
        ResultDescriptor rDescriptor = new ResultDescriptor(m_config.getInterval());

        for (String metric : m_config.getMetrics()) {
            rDescriptor.datasource(metric, metric, m_config.getHeartbeat(), StandardAggregationFunctions.AVERAGE);
        }

        rDescriptor.export(m_config.getMetrics());

        return rDescriptor;
    }

    private void createThreads() {
        for (int i = 0; i < m_config.getThreads(); i++) {
            m_threads[i] = new Selecter(i, m_repository, getResultDescriptor(), m_queryQueue, m_metricRegistry);
        }
    }

    @Override
    void go() throws InterruptedException {

        createThreads();

        for (Timestamp t : new IntervalGenerator(m_config.getStart(), m_config.getEnd(), m_config.getSelectLength(), true)) {
            for (String resource : m_config.getResources()) {
                m_queryQueue.put(new Query(resource, t.minus(m_config.getSelectLength()), t, m_config.getResolution()));
            }
        }

        shutdown();

    }

}
