/*
 * Copyright 2015-2024, The OpenNMS Group
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


import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.datastax.oss.driver.api.core.cql.AsyncResultSet;
import com.datastax.oss.driver.api.core.cql.BatchStatement;
import com.datastax.oss.driver.api.core.cql.BatchStatementBuilder;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.DefaultBatchType;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.querybuilder.delete.Delete;
import com.datastax.oss.driver.api.querybuilder.insert.RegularInsert;
import com.datastax.oss.driver.api.querybuilder.select.Select;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;

import org.opennms.newts.aggregate.IntervalGenerator;
import org.opennms.newts.aggregate.ResultProcessor;
import org.opennms.newts.api.Context;
import org.opennms.newts.api.Duration;
import org.opennms.newts.api.Measurement;
import org.opennms.newts.api.Resource;
import org.opennms.newts.api.Results;
import org.opennms.newts.api.Results.Row;
import org.opennms.newts.api.Sample;
import org.opennms.newts.api.SampleProcessorService;
import org.opennms.newts.api.SampleRepository;
import org.opennms.newts.api.SampleSelectCallback;
import org.opennms.newts.api.Timestamp;
import org.opennms.newts.api.ValueType;
import org.opennms.newts.api.query.ResultDescriptor;
import org.opennms.newts.cassandra.CassandraSession;
import org.opennms.newts.cassandra.ContextConfigurations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static com.codahale.metrics.MetricRegistry.name;
import static com.datastax.oss.driver.api.querybuilder.QueryBuilder.bindMarker;
import static com.datastax.oss.driver.api.querybuilder.QueryBuilder.deleteFrom;
import static com.datastax.oss.driver.api.querybuilder.QueryBuilder.insertInto;
import static com.datastax.oss.driver.api.querybuilder.QueryBuilder.literal;
import static com.datastax.oss.driver.api.querybuilder.QueryBuilder.selectFrom;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;


public class CassandraSampleRepository implements SampleRepository {

    private static final Logger LOG = LoggerFactory.getLogger(CassandraSampleRepository.class);

    // Used to calculate the duration when the duration is not specified
    private static final int TARGET_NUMBER_OF_STEPS = 10;
    private static final int DELETION_INTERVAL = 360;

    private final CassandraSession m_session;
    private final int m_ttl;
    private final SampleProcessorService m_processorService;
    private final PreparedStatement m_selectStatement;
    private final PreparedStatement m_deleteStatement;

    private final Timer m_sampleSelectTimer;
    private final Timer m_measurementSelectTimer;
    private final Timer m_insertTimer;
    private final Meter m_samplesInserted;
    private final Meter m_samplesSelected;

    private final ContextConfigurations m_contextConfigurations;

    @Inject
    public CassandraSampleRepository(CassandraSession session, @Named("samples.cassandra.time-to-live") int ttl, @Named("newtsMetricRegistry") MetricRegistry registry,
                                     SampleProcessorService processorService, ContextConfigurations contextConfigurations) {

        m_session = checkNotNull(session, "session argument");
        checkArgument(ttl >= 0, "Negative Cassandra column TTL");

        m_ttl = ttl;

        checkNotNull(registry, "metric registry argument");
        m_processorService = processorService;

        m_contextConfigurations = checkNotNull(contextConfigurations, "contextConfigurations argument");

        Select select = selectFrom(SchemaConstants.T_SAMPLES).columns(SchemaConstants.F_VALUE, SchemaConstants.F_METRIC_NAME, SchemaConstants.F_COLLECTED, SchemaConstants.F_RESOURCE, SchemaConstants.F_ATTRIBUTES)
                .whereColumn(SchemaConstants.F_CONTEXT).isEqualTo(bindMarker(SchemaConstants.F_CONTEXT))
                .whereColumn(SchemaConstants.F_PARTITION).isEqualTo(bindMarker(SchemaConstants.F_PARTITION))
                .whereColumn(SchemaConstants.F_RESOURCE).isEqualTo(bindMarker(SchemaConstants.F_RESOURCE))
                .whereColumn(SchemaConstants.F_COLLECTED).isGreaterThanOrEqualTo(bindMarker("start"))
                .whereColumn(SchemaConstants.F_COLLECTED).isLessThanOrEqualTo(bindMarker("end"));
        m_selectStatement = m_session.prepare(select.toString());

        Delete delete = deleteFrom(SchemaConstants.T_SAMPLES)
                .whereColumn(SchemaConstants.F_CONTEXT).isEqualTo(bindMarker(SchemaConstants.F_CONTEXT))
                .whereColumn(SchemaConstants.F_PARTITION).isEqualTo(bindMarker(SchemaConstants.F_PARTITION))
                .whereColumn(SchemaConstants.F_RESOURCE).isEqualTo(bindMarker(SchemaConstants.F_RESOURCE));
        m_deleteStatement = m_session.prepare(delete.toString());

        m_sampleSelectTimer = registry.timer(metricName("sample-select-timer"));
        m_measurementSelectTimer = registry.timer(metricName("measurement-select-timer"));
        m_insertTimer = registry.timer(metricName("insert-timer"));
        m_samplesInserted = registry.meter(metricName("samples-inserted"));
        m_samplesSelected = registry.meter(metricName("samples-selected"));
    }

    public Iterable<Results.Row<Sample>> select(Context context, Resource resource, Timestamp start, Timestamp end, ResultDescriptor descriptor, Duration step) {
        return new DriverAdapter(cassandraSelect(context, resource, start.minus(step), end),
                descriptor.getSourceNames());
    }

    @Override
    public Results<Measurement> select(Context context, Resource resource, Optional<Timestamp> start, Optional<Timestamp> end, ResultDescriptor descriptor, Optional<Duration> resolution) {
        return select(context, resource, start, end, descriptor, resolution, noopSampleSelectCallback);
    }

    @Override
    public Results<Measurement> select(Context context, Resource resource, Optional<Timestamp> start, Optional<Timestamp> end, ResultDescriptor descriptor, Optional<Duration> resolution, SampleSelectCallback callback) {

        Timer.Context timer = m_measurementSelectTimer.time();

        validateSelect(start, end);

        Timestamp upper = end.isPresent() ? end.get() : Timestamp.now();
        Timestamp lower = start.isPresent() ? start.get() : upper.minus(Duration.seconds(86400));
        Duration step;
        if (resolution.isPresent()) {
            step = resolution.get();
        } else {
            // Determine the ideal step size, splitting the interval evenly into N slices
            long stepMillis = upper.minus(lower).asMillis() / TARGET_NUMBER_OF_STEPS;

            // But every step must be a multiple of the interval
            long intervalMillis = descriptor.getInterval().asMillis();

            // If the interval is greater than the target step, use the 2 * interval as the step
            if (intervalMillis >= stepMillis) {
                step = descriptor.getInterval().times(2);
            } else {
                // Otherwise, round stepMillkeyis up to the closest multiple of intervalMillis
                long remainderMillis = stepMillis % intervalMillis;
                if (remainderMillis != 0) {
                    stepMillis = stepMillis + intervalMillis - remainderMillis;
                }

                step = Duration.millis(stepMillis);
            }
        }

        LOG.debug("Querying database for resource {}, from {} to {}", resource, lower.minus(step), upper);

        DriverAdapter driverAdapter = new DriverAdapter(cassandraSelect(context, resource, lower.minus(step), upper),
                    descriptor.getSourceNames());

        Results<Measurement> results;
        callback.beforeProcess();
        try {
            results = new ResultProcessor(resource, lower, upper, descriptor, step).process(driverAdapter);
        } finally {
            callback.afterProcess();
        }

        LOG.debug("{} results returned from database", driverAdapter.getResultCount());
        m_samplesSelected.mark(driverAdapter.getResultCount());

        try {
            return results;
        } finally {
            timer.stop();
        }

    }

    @Override
    public Results<Sample> select(Context context, Resource resource, Optional<Timestamp> start, Optional<Timestamp> end) {

        Timer.Context timer = m_sampleSelectTimer.time();

        validateSelect(start, end);

        Timestamp upper = end.isPresent() ? end.get() : Timestamp.now();
        Timestamp lower = start.isPresent() ? start.get() : upper.minus(Duration.seconds(86400));

        LOG.debug("Querying database for resource {}, from {} to {}", resource, lower, upper);

        Results<Sample> samples = new Results<>();
        DriverAdapter driverAdapter = new DriverAdapter(cassandraSelect(context, resource, lower, upper));

        for (Row<Sample> row : driverAdapter) {
            samples.addRow(row);
        }

        LOG.debug("{} results returned from database", driverAdapter.getResultCount());
        m_samplesSelected.mark(driverAdapter.getResultCount());

        try {
            return samples;
        } finally {
            timer.stop();
        }
    }

    @Override
    public void insert(Collection<Sample> samples) {
        insert(samples, false);
    }

    @Override
    public void insert(Collection<Sample> samples, boolean calculateTimeToLive) {

        Timer.Context timer = m_insertTimer.time();
        Timestamp now = Timestamp.now();

        BatchStatementBuilder batchBuilder = BatchStatement.builder(DefaultBatchType.UNLOGGED);

        for (Sample m : samples) {
            int ttl = m_ttl;
            if (calculateTimeToLive) {
                ttl -= (int) (now.asSeconds() - m.getTimestamp().asSeconds());
                if (ttl <= 0) {
                    LOG.debug("Skipping expired sample: {}", m);
                    continue;
                }
            }

            Duration resourceShard = m_contextConfigurations.getResourceShard(m.getContext());

            RegularInsert insert = insertInto(SchemaConstants.T_SAMPLES)
                    .value(SchemaConstants.F_CONTEXT, literal(m.getContext().getId()))
                    .value(SchemaConstants.F_PARTITION, literal(m.getTimestamp().stepFloor(resourceShard).asSeconds()))
                    .value(SchemaConstants.F_RESOURCE, literal(m.getResource().getId()))
                    .value(SchemaConstants.F_COLLECTED, literal(m.getTimestamp().asMillis()))
                    .value(SchemaConstants.F_METRIC_NAME, literal(m.getName()))
                    .value(SchemaConstants.F_VALUE, literal(ValueType.decompose(m.getValue())));

            // Inserting a column with a null value inserts a tombstone (a deletion marker); Skip the attributes
            // for any sample that has not specified them.
            if (m.getAttributes() != null) {
                insert = insert.value(SchemaConstants.F_ATTRIBUTES, literal(m.getAttributes()));
            }

            // Use the context specific consistency level
            batchBuilder.addStatement(insert.usingTtl(ttl).build()
                    .setConsistencyLevel(m_contextConfigurations.getWriteConsistency(m.getContext())));
        }

        try {
            m_session.execute(batchBuilder.build());

            if (m_processorService != null) {
                m_processorService.submit(samples);
            }

            m_samplesInserted.mark(samples.size());
        } finally {
            timer.stop();
        }
    }

    @Override
    public void delete(Context context, Resource resource) {
        /**
         * Check for ttl value > 0
         */
        if (m_ttl > 0) {
            /**
             * Delete exactly from (now - ttl) till now
             */
            final Timestamp start = Timestamp.now().minus(m_ttl, TimeUnit.SECONDS);
            final Timestamp end = Timestamp.now();

            final Duration resourceShard = m_contextConfigurations.getResourceShard(context);

            final List<CompletionStage<AsyncResultSet>> futures = Lists.newArrayList();
            for (Timestamp partition : new IntervalGenerator(start.stepFloor(resourceShard),
                    end.stepFloor(resourceShard),
                    resourceShard)) {
                BoundStatement bindStatement = m_deleteStatement.bind();
                bindStatement.setString(SchemaConstants.F_CONTEXT, context.getId());
                bindStatement.setInt(SchemaConstants.F_PARTITION, (int) partition.asSeconds());
                bindStatement.setString(SchemaConstants.F_RESOURCE, resource.getId());

                futures.add(m_session.executeAsync(bindStatement));
            }

            for (final CompletionStage<AsyncResultSet> future : futures) {
                try {
                    future.toCompletableFuture().get();
                } catch (final InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
            }
        } else {
            // Choose (now - one year) till now...
            Timestamp end = Timestamp.now();
            Timestamp start = end.minus(DELETION_INTERVAL, TimeUnit.DAYS);

            // ... and check whether samples exist for this period of time.
            while (cassandraSelect(context, resource, start, end).hasNext()) {
                // Now delete the samples...
                final Duration resourceShard = m_contextConfigurations.getResourceShard(context);

                final List<CompletionStage<AsyncResultSet>> futures = Lists.newArrayList();
                for (Timestamp partition : new IntervalGenerator(start.stepFloor(resourceShard),
                        end.stepFloor(resourceShard),
                        resourceShard)) {
                    BoundStatement bindStatement = m_deleteStatement.bind();
                    bindStatement.setString(SchemaConstants.F_CONTEXT, context.getId());
                    bindStatement.setInt(SchemaConstants.F_PARTITION, (int) partition.asSeconds());
                    bindStatement.setString(SchemaConstants.F_RESOURCE, resource.getId());

                    futures.add(m_session.executeAsync(bindStatement));
                }

                for (final CompletionStage<AsyncResultSet> future : futures) {
                    try {
                        future.toCompletableFuture().get();
                    } catch (final InterruptedException | ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                }

                // ...set end to start and start to (end - one year)
                end = start;
                start = end.minus(DELETION_INTERVAL, TimeUnit.DAYS);

                // and start over again until no more samples are found
            }
        }
    }

    private Iterator<com.datastax.oss.driver.api.core.cql.Row> cassandraSelect(Context context, Resource resource,
                                                                               Timestamp start, Timestamp end) {

        List<CompletionStage<AsyncResultSet>> futures = Lists.newArrayList();

        Duration resourceShard = m_contextConfigurations.getResourceShard(context);
        Timestamp lower = start.stepFloor(resourceShard);
        Timestamp upper = end.stepFloor(resourceShard);

        for (Timestamp partition : new IntervalGenerator(lower, upper, resourceShard)) {
            BoundStatement bindStatement = m_selectStatement.bind()
                    .setString(SchemaConstants.F_CONTEXT, context.getId())
                    .setInt(SchemaConstants.F_PARTITION, (int) partition.asSeconds())
                    .setString(SchemaConstants.F_RESOURCE, resource.getId())
                    .setInstant("start", start.asDate().toInstant())
                    .setInstant("end", end.asDate().toInstant())
                    .setConsistencyLevel(m_contextConfigurations.getReadConsistency(context));
            futures.add(m_session.executeAsync(bindStatement));
        }

        return new ConcurrentResultWrapper(futures);
    }

    private void validateSelect(Optional<Timestamp> start, Optional<Timestamp> end) {
        if ((start.isPresent() && end.isPresent()) && start.get().gt(end.get())) {
            throw new IllegalArgumentException("start time must be less than end time");
        }
    }

    private String metricName(String suffix) {
        return name("repository", suffix);
    }

    private static final SampleSelectCallback noopSampleSelectCallback = new SampleSelectCallback() {

        @Override
        public void beforeProcess() {
            // pass
        }

        @Override
        public void afterProcess() {
            // pass
        }
    };
}
