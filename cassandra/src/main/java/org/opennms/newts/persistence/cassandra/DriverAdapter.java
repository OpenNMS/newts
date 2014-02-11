package org.opennms.newts.persistence.cassandra;


import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Iterator;

import org.opennms.newts.api.Measurement;
import org.opennms.newts.api.MetricType;
import org.opennms.newts.api.Results;
import org.opennms.newts.api.Results.Row;
import org.opennms.newts.api.Timestamp;
import org.opennms.newts.api.ValueType;

import com.datastax.driver.core.ResultSet;


public class DriverAdapter implements Iterable<Results.Row>, Iterator<Results.Row> {

    private final Iterator<com.datastax.driver.core.Row> m_results;

    private Results.Row m_next = null;

    public DriverAdapter(ResultSet input) {
        checkNotNull(input, "input argument");
        m_results = input.iterator();

        if (m_results.hasNext()) {
            Measurement m = getMeasurement(m_results.next());
            m_next = new Results.Row(m.getTimestamp(), m.getResource());
            m_next.addMeasurement(m);
        }
    }

    @Override
    public boolean hasNext() {
        return m_next != null;
    }

    @Override
    public Results.Row next() {
        Results.Row nextNext = null;

        while (m_results.hasNext()) {
            Measurement m = getMeasurement(m_results.next());

            if (m.getTimestamp().gt(m_next.getTimestamp())) {
                nextNext = new Results.Row(m.getTimestamp(), m.getResource());
                nextNext.addMeasurement(m);
                break;
            }

            m_next.addMeasurement(m);
        }

        try {
            return m_next;
        }
        finally {
            m_next = nextNext;
        }
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<Row> iterator() {
        return this;
    }

    private Measurement getMeasurement(com.datastax.driver.core.Row row) {
        MetricType type = getMetricType(row);
        return new Measurement(getTimestamp(row), getResource(row), getMetricName(row), type, getValue(row, type));
    }

    private ValueType<?> getValue(com.datastax.driver.core.Row row, MetricType type) {
        return ValueType.compose(row.getBytes(SchemaConstants.F_VALUE), type);
    }

    private MetricType getMetricType(com.datastax.driver.core.Row row) {
        return MetricType.valueOf(row.getString(SchemaConstants.F_METRIC_TYPE));
    }

    private String getMetricName(com.datastax.driver.core.Row row) {
        return row.getString(SchemaConstants.F_METRIC_NAME);
    }

    private Timestamp getTimestamp(com.datastax.driver.core.Row row) {
        return Timestamp.fromEpochMillis(row.getDate(SchemaConstants.F_COLLECTED).getTime());
    }

    private String getResource(com.datastax.driver.core.Row row) {
        return row.getString(SchemaConstants.F_RESOURCE);
    }

}
