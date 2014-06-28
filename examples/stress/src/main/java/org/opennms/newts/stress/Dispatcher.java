package org.opennms.newts.stress;


import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.concurrent.BlockingQueue;

import org.opennms.newts.api.Sample;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.Queues;


abstract class Dispatcher {

    protected final Config m_config;
    protected final BlockingQueue<Collection<Sample>> m_samplesQueue;
    protected final Worker[] m_threads;
    protected final MetricRegistry m_metricRegistry = new MetricRegistry();

    Dispatcher(Config config) {
        m_config = checkNotNull(config, "config argument");
        m_samplesQueue = Queues.newArrayBlockingQueue(config.getThreads() * 10);
        m_threads = new Worker[m_config.getThreads()];
    }

    abstract void go() throws InterruptedException;

    void shutdown() throws InterruptedException {
        // Trigger shutdown on all threads
        for (Worker w : m_threads) {
            w.shutdown();
        }

        for (Worker w : m_threads) {
            w.join();
        }

    }

    void printReport() {
        ConsoleReporter.forRegistry(m_metricRegistry).build().report();
    }

}
