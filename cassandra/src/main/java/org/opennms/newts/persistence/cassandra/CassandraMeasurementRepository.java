package org.opennms.newts.persistence.cassandra;


import static com.codahale.metrics.MetricRegistry.name;
import static com.datastax.driver.core.querybuilder.QueryBuilder.batch;
import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.gte;
import static com.datastax.driver.core.querybuilder.QueryBuilder.insertInto;
import static com.datastax.driver.core.querybuilder.QueryBuilder.lt;

import java.util.Collection;
import java.util.Map;

import javax.inject.Named;

import org.opennms.newts.api.AggregateFunctions;
import org.opennms.newts.api.AggregateFunctions.Point;
import org.opennms.newts.api.Aggregates;
import org.opennms.newts.api.Duration;
import org.opennms.newts.api.Measurement;
import org.opennms.newts.api.MeasurementRepository;
import org.opennms.newts.api.MetricType;
import org.opennms.newts.api.Results;
import org.opennms.newts.api.Timestamp;
import org.opennms.newts.api.ValueType;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.Batch;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.google.common.base.Optional;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.inject.Inject;


public class CassandraMeasurementRepository implements MeasurementRepository {

    private static final String T_MEASUREMENTS = "measurements";

    private static final String F_RESOURCE = "resource";
    private static final String F_COLLECTED = "collected_at";
    private static final String F_METRIC_NAME = "metric_name";
    private static final String F_METRIC_TYPE = "metric_type";
    private static final String F_VALUE = "value";
    private static final String F_ATTRIBUTES = "attributes";

    private Session m_session;
    private MetricRegistry m_registry;
    private Timer m_timerAvgCalc;
    private Timer m_timerRateCalc;

    @Inject
    public CassandraMeasurementRepository(@Named("cassandraKeyspace") String keyspace, @Named("cassandraHost") String host, @Named("cassandraPort") int port, MetricRegistry registry) {

        Cluster cluster = Cluster.builder().withPort(port).addContactPoint(host).build();
        m_session = cluster.connect(keyspace);

        m_registry = registry;

        m_timerAvgCalc = m_registry.timer(name(CassandraMeasurementRepository.class, "aggregate", "average"));
        m_timerRateCalc = m_registry.timer(name(CassandraMeasurementRepository.class, "aggregate", "rate"));

    }

    @Override
    public Results select(String resource, Optional<Timestamp> start, Optional<Timestamp> end, Aggregates aggregates) {

        Timestamp upper = end.isPresent() ? end.get() : Timestamp.now();
        Timestamp lower = start.isPresent() ? start.get() : upper.minus(Duration.seconds(86400));

        Multimap<String, Point> points = ArrayListMultimap.create();
        Map<String, MetricType> metricTypes = Maps.newHashMap();

        // Collate results
        for (Row row : cassandraSelect(resource, lower, upper)) {

            String name = getMetricName(row);
            MetricType type = getMetricType(row);

            if (!metricTypes.containsKey(name)) {
                metricTypes.put(name, type);
            }
            else if (!type.equals(metricTypes.get(name))) {
                String msg = String.format("encountered %s metric while processing type %s", type, metricTypes.get(name));
                throw new RuntimeException(msg);
            }

            points.put(name, new Point(getTimestamp(row), getValue(row, type)));
        }


        Results measurements = new Results();

        // Perform aggregations (rate, average, etc), and construct results.
        for (String name : points.keySet()) {

            Collection<Point> aggregated = points.get(name);

            if (metricTypes.get(name).equals(MetricType.COUNTER)) {
                aggregated = rate(aggregated);
            }

            average(lower, upper, aggregates.getStep(), aggregated);

            for (Point point : aggregated) {
                measurements.addMeasurement(new Measurement(point.x, resource, name, metricTypes.get(name), point.y));
            }
        }

        return measurements;
    }

    @Override
    public Results select(String resource, Optional<Timestamp> start, Optional<Timestamp> end) {

        Timestamp upper = end.isPresent() ? end.get() : Timestamp.now();
        Timestamp lower = start.isPresent() ? start.get() : upper.minus(Duration.seconds(86400));

        Results measurements = new Results();

        for (Row row : cassandraSelect(resource, lower, upper)) {
            measurements.addMeasurement(getMeasurement(row));
        }

        return measurements;
    }

    @Override
    public void insert(Collection<Measurement> measurements) {

        Batch batch = batch();

        for (Measurement m : measurements) {
            batch.add(
                    insertInto(T_MEASUREMENTS)
                        .value(F_RESOURCE, m.getResource())
                        .value(F_COLLECTED, m.getTimestamp().asMillis())
                        .value(F_METRIC_NAME, m.getName())
                        .value(F_METRIC_TYPE, m.getType().toString())
                        .value(F_VALUE, ValueType.decompose(m.getValue()))
                        .value(F_ATTRIBUTES, m.getAttributes())
            );
        }

        m_session.execute(batch);

    }

    private Collection<Point> rate(Collection<Point> points) {
        Context ctx = m_timerRateCalc.time();

        try {
            return AggregateFunctions.rate(points);
        }
        finally {
            ctx.stop();
        }
    }

    private Collection<Point> average(Timestamp start, Timestamp end, Duration stepSize, Collection<Point> points) {
        Context ctx = m_timerAvgCalc.time();

        try {
            return AggregateFunctions.average(start, end, stepSize, points);
        }
        finally {
            ctx.stop();
        }
    }

    private Measurement getMeasurement(Row row) {
        MetricType type = getMetricType(row);
        return new Measurement(getTimestamp(row), getResource(row), getMetricName(row), type, getValue(row, type));
    }

    private ValueType<?> getValue(Row row, MetricType type) {
        return ValueType.compose(row.getBytes(F_VALUE), type);
    }

    private MetricType getMetricType(Row row) {
        return MetricType.valueOf(row.getString(F_METRIC_TYPE));
    }

    private String getMetricName(Row row) {
        return row.getString(F_METRIC_NAME);
    }

    private Timestamp getTimestamp(Row row) {
        return Timestamp.fromEpochMillis(row.getDate(F_COLLECTED).getTime());
    }

    private String getResource(Row row) {
        return row.getString(F_RESOURCE);
    }

    // FIXME: Use a prepared statement for this.
    private ResultSet cassandraSelect(String resource, Timestamp start, Timestamp end) {

        Select select = QueryBuilder.select().from(T_MEASUREMENTS);
        select.where(eq(F_RESOURCE, resource));

        select.where(gte(F_COLLECTED, start.asDate()));
        select.where(lt(F_COLLECTED, end.asDate()));

        return m_session.execute(select);
    }

}
