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
package org.opennms.newts.api;


import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;

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

    @Inject
    public SampleProcessorService(@Named("sampleProcessor.maxThreads") int maxThreads, Set<SampleProcessor> processors) {
        checkArgument(maxThreads > 0, "maxThreads must be non-zero");

        LOG.info("Starting sample processor service with pool of {} threads", maxThreads);

        m_executor = new BlockingThreadPoolExecutor(1, maxThreads, 61, TimeUnit.SECONDS);
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
     * @param timeout
     *            the maximum time to wait
     * @param unit
     *            the time unit of the timeout argument
     * @return <tt>true</tt> if this executor terminated and <tt>false</tt> if the timeout elapsed
     *         before termination
     * @throws InterruptedException
     */
    public boolean awaitShutdown(long timeout, TimeUnit unit) throws InterruptedException {
        return m_executor.awaitTermination(timeout, unit);
    }

}
