package org.opennms.newts.api;


import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Distributes incoming sample collections to one or more sample processors. The sample processors
 * are run on a {@link BlockingThreadPoolExecutor}.
 *
 * @author eevans
 */
public class SampleProcessorService {

    private static final Logger LOG = LoggerFactory.getLogger(SampleProcessorService.class);

    private final ThreadPoolExecutor m_executor;
    private final Set<SampleProcessor> m_processors;

    public SampleProcessorService(int maxThreads) {
        this(maxThreads, Collections.<SampleProcessor> emptySet());
    }

    public SampleProcessorService(int maxThreads, Set<SampleProcessor> processors) {
        checkArgument(maxThreads > 0, "maxThreads must be non-zero");

        LOG.info("Starting sample processor service with pool of {} threads", maxThreads);

        m_executor = new BlockingThreadPoolExecutor(1, maxThreads, 301, TimeUnit.SECONDS);
        m_processors = checkNotNull(processors, "processors argument");

    }

    /**
     * Submits a collection of samples to each of the underlying {@link SampleProcessor}s. Calls to
     * {@link SampleProcessorService#submit(Collection)} are non-blocking unless the pool is unable
     * to keep up and the queue becomes full.
     *
     * @param samples
     */
    public void submit(final Collection<Sample> samples) {
        for (final SampleProcessor processor : m_processors) {
            m_executor.execute(new Runnable() {

                @Override
                public void run() {
                    processor.submit(samples);
                }
            });
        }
    }

    /** Calls <code>shutdown()</code> on the underlying thread pool executor. */
    public void shutdown() throws InterruptedException {
        LOG.info("Shutting down thread pool executor");
        m_executor.shutdown();
    }

    /**
     * Invokes <code>awaitShutdown()</code> on the underlying thread pool executor.
     *
     * @throws InterruptedException
     */
    public void awaitShutdown() throws InterruptedException {
        m_executor.awaitTermination(20, TimeUnit.SECONDS);
    }

}
