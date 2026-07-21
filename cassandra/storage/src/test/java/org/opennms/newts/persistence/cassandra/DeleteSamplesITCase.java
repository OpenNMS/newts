/*
 * Copyright 2026, The OpenNMS Group
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
package org.opennms.newts.persistence.cassandra;


import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.opennms.newts.api.MetricType.GAUGE;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.opennms.newts.api.Context;
import org.opennms.newts.api.Gauge;
import org.opennms.newts.api.Resource;
import org.opennms.newts.api.Sample;
import org.opennms.newts.api.SampleProcessorService;
import org.opennms.newts.api.Results.Row;
import org.opennms.newts.api.Timestamp;

import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;


public class DeleteSamplesITCase extends NewtsSampleRepositoryTestCase {

    private static final int NUM_SAMPLES = 10;

    /**
     * Exercises the {@code m_ttl > 0} branch of {@link CassandraSampleRepository#delete}, which
     * deletes the partitions covering the window {@code (now - ttl) .. now}. The base test case
     * configures a positive TTL ({@link #CASSANDRA_TTL}), so the samples must be written with
     * recent timestamps to land inside the deletion window.
     */
    @Test
    public void deletesSamplesWithinTtlWindow() {
        Resource resource = new Resource("r-ttl");

        insertRecentSamples(getRepository(), resource);
        assertEquals("Samples should exist prior to deletion", NUM_SAMPLES, countSamples(getRepository(), resource));

        getRepository().delete(Context.DEFAULT_CONTEXT, resource);

        assertEquals("All samples should have been deleted", 0, countSamples(getRepository(), resource));
    }

    /**
     * Exercises the {@code else} branch of {@link CassandraSampleRepository#delete} (TTL of zero),
     * which walks backwards in {@code DELETION_INTERVAL}-day windows from {@code now} until no more
     * samples are found. Uses a dedicated repository constructed with a TTL of {@code 0}.
     */
    @Test
    public void deletesSamplesWhenTtlIsZero() {
        CassandraSampleRepository repository = new CassandraSampleRepository(
                newtsInstance.getCassandraSession(),
                0,
                new MetricRegistry(),
                mock(SampleProcessorService.class),
                m_contextConfigurations);

        Resource resource = new Resource("r-nottl");

        insertRecentSamples(repository, resource);
        assertEquals("Samples should exist prior to deletion", NUM_SAMPLES, countSamples(repository, resource));

        repository.delete(Context.DEFAULT_CONTEXT, resource);

        assertEquals("All samples should have been deleted", 0, countSamples(repository, resource));
    }

    /** Inserts {@link #NUM_SAMPLES} samples at distinct, recent timestamps (one metric per row). */
    private static void insertRecentSamples(CassandraSampleRepository repository, Resource resource) {
        Timestamp now = Timestamp.now();
        List<Sample> samples = Lists.newArrayList();
        for (int i = 1; i <= NUM_SAMPLES; i++) {
            Timestamp ts = now.minus(i, TimeUnit.SECONDS);
            samples.add(new Sample(ts, resource, "m0", GAUGE, new Gauge(i)));
        }
        repository.insert(samples);
    }

    /** Counts the sample rows currently stored for the given resource over a wide, recent window. */
    private static int countSamples(CassandraSampleRepository repository, Resource resource) {
        Timestamp end = Timestamp.now().plus(1, TimeUnit.MINUTES);
        Timestamp start = end.minus(1, TimeUnit.DAYS);
        Iterator<Row<Sample>> results = repository.select(Context.DEFAULT_CONTEXT, resource,
                Optional.of(start), Optional.of(end)).iterator();
        int count = 0;
        while (results.hasNext()) {
            results.next();
            count++;
        }
        return count;
    }

}