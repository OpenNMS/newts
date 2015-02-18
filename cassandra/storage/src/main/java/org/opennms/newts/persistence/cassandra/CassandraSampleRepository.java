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
package org.opennms.newts.persistence.cassandra;


import static com.codahale.metrics.MetricRegistry.name;
import static com.datastax.driver.core.querybuilder.QueryBuilder.bindMarker;
import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.gte;
import static com.datastax.driver.core.querybuilder.QueryBuilder.insertInto;
import static com.datastax.driver.core.querybuilder.QueryBuilder.lte;
import static com.datastax.driver.core.querybuilder.QueryBuilder.ttl;
import static com.datastax.driver.core.querybuilder.QueryBuilder.unloggedBatch;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Future;

import javax.inject.Inject;
import javax.inject.Named;

import org.opennms.newts.aggregate.IntervalGenerator;
import org.opennms.newts.aggregate.ResultProcessor;
import org.opennms.newts.api.Duration;
import org.opennms.newts.api.Measurement;
import org.opennms.newts.api.Resource;
import org.opennms.newts.api.Results;
import org.opennms.newts.api.Results.Row;
import org.opennms.newts.api.Sample;
import org.opennms.newts.api.SampleProcessorService;
import org.opennms.newts.api.SampleRepository;
import org.opennms.newts.api.Timestamp;
import org.opennms.newts.api.ValueType;
import org.opennms.newts.api.query.ResultDescriptor;
import org.opennms.newts.cassandra.CassandraSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.querybuilder.Batch;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;


public class CassandraSampleRepository implements SampleRepository {

    private static final Logger LOG = LoggerFactory.getLogger(CassandraSampleRepository.class);

    private final CassandraSession m_session;
    private final int m_ttl;
    private final SampleProcessorService m_processorService;
    private final PreparedStatement m_selectStatement;

    private final Timer m_sampleSelectTimer;
    private final Timer m_measurementSelectTimer;
    private final Timer m_insertTimer;

    private Duration m_resourceShard = Duration.seconds(600000);

    @Inject
    public CassandraSampleRepository(CassandraSession session, @Named("samples.cassandra.time-to-live") int ttl, MetricRegistry registry, SampleProcessorService processorService) {

        m_session = checkNotNull(session, "session argument");
        checkArgument(ttl >= 0, "Negative Cassandra column TTL");

        m_ttl = ttl;

        checkNotNull(registry, "metric registry argument");
        m_processorService = processorService;

        Select select = QueryBuilder.select().from(SchemaConstants.T_SAMPLES);
        select.where(eq(SchemaConstants.F_PARTITION, bindMarker(SchemaConstants.F_PARTITION)));
        select.where(eq(SchemaConstants.F_RESOURCE, bindMarker(SchemaConstants.F_RESOURCE)));

        select.where(gte(SchemaConstants.F_COLLECTED, bindMarker("start")));
        select.where(lte(SchemaConstants.F_COLLECTED, bindMarker("end")));

        m_selectStatement = m_session.prepare(select.toString());
        
        m_sampleSelectTimer = registry.timer(metricName("sample-select-timer"));
        m_measurementSelectTimer = registry.timer(metricName("measurement-select-timer"));
        m_insertTimer = registry.timer(metricName("insert-timer"));

    }

    @Override
    public Results<Measurement> select(Resource resource, Optional<Timestamp> start, Optional<Timestamp> end, ResultDescriptor descriptor, Duration resolution) {

        Context context = m_measurementSelectTimer.time();

        validateSelect(start, end);

        Timestamp upper = end.isPresent() ? end.get() : Timestamp.now();
        Timestamp lower = start.isPresent() ? start.get() : upper.minus(Duration.seconds(86400));

        LOG.debug("Querying database for resource {}, from {} to {}", resource, lower.minus(resolution), upper);

        DriverAdapter driverAdapter = new DriverAdapter(cassandraSelect(resource, lower.minus(resolution), upper), descriptor.getSourceNames());
        Results<Measurement> results = new ResultProcessor(resource, lower, upper, descriptor, resolution).process(driverAdapter);

        LOG.debug("{} results returned from database", driverAdapter.getResultCount());

        try {
            return results;
        }
        finally {
            context.stop();
        }

    }

    @Override
    public Results<Sample> select(Resource resource, Optional<Timestamp> start, Optional<Timestamp> end) {

        Context context = m_sampleSelectTimer.time();

        validateSelect(start, end);

        Timestamp upper = end.isPresent() ? end.get() : Timestamp.now();
        Timestamp lower = start.isPresent() ? start.get() : upper.minus(Duration.seconds(86400));

        LOG.debug("Querying database for resource {}, from {} to {}", resource, lower, upper);

        Results<Sample> samples = new Results<Sample>();
        DriverAdapter driverAdapter = new DriverAdapter(cassandraSelect(resource, lower, upper));

        for (Row<Sample> row : driverAdapter) {
            samples.addRow(row);
        }

        LOG.debug("{} results returned from database", driverAdapter.getResultCount());

        try {
            return samples;
        }
        finally {
            context.stop();
        }
    }

    @Override
    public void insert(Collection<Sample> samples) {

        Context context = m_insertTimer.time();

        Batch batch = unloggedBatch();

        for (Sample m : samples) {
            batch.add(
                    insertInto(SchemaConstants.T_SAMPLES)
                        .value(SchemaConstants.F_PARTITION, m.getTimestamp().stepFloor(m_resourceShard).asSeconds())
                        .value(SchemaConstants.F_RESOURCE, m.getResource().getId())
                        .value(SchemaConstants.F_COLLECTED, m.getTimestamp().asMillis())
                        .value(SchemaConstants.F_METRIC_NAME, m.getName())
                        .value(SchemaConstants.F_VALUE, ValueType.decompose(m.getValue()))
                        .value(SchemaConstants.F_ATTRIBUTES, m.getAttributes())
                        .using(ttl(m_ttl))
            );
        }

        try {
            m_session.execute(batch);

            if (m_processorService != null) {
                m_processorService.submit(samples);
            }
        }
        finally {
            context.stop();
        }

    }

    private Iterator<com.datastax.driver.core.Row> cassandraSelect(Resource resource, Timestamp start, Timestamp end) {

        List<Future<ResultSet>> futures = Lists.newArrayList();

        Timestamp lower = start.stepFloor(m_resourceShard);
        Timestamp upper = end.stepFloor(m_resourceShard);

        for (Timestamp partition : new IntervalGenerator(lower, upper, m_resourceShard)) {
            BoundStatement bindStatement = m_selectStatement.bind();
            bindStatement.setInt(SchemaConstants.F_PARTITION, (int) partition.asSeconds());
            bindStatement.setString(SchemaConstants.F_RESOURCE, resource.getId());
            bindStatement.setDate("start", start.asDate());
            bindStatement.setDate("end", end.asDate());

            futures.add(m_session.executeAsync(bindStatement));
        }

        return new ConcurrentResultWrapper(futures);
    }

    private void validateSelect(Optional<Timestamp> start, Optional<Timestamp> end) {
        if ((start.isPresent() && end.isPresent()) && start.get().gt(end.get())) {
            throw new IllegalArgumentException("start time must be less than end time");
        }
    }

    // Use only in tests!
    void setResourceShard(Duration resourceShard) {
        m_resourceShard = resourceShard;
    }

    private String metricName(String suffix) {
        return name("repository", suffix);
    }

}
