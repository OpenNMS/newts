package org.opennms.newts.persistence.cassandra;


import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

import org.opennms.newts.api.Results;
import org.opennms.newts.api.Sample;
import org.opennms.newts.api.MetricType;
import org.opennms.newts.api.Results.Row;
import org.opennms.newts.api.Timestamp;
import org.opennms.newts.api.ValueType;
import org.opennms.newts.api.query.Datasource;
import org.opennms.newts.api.query.ResultDescriptor;

import com.datastax.driver.core.ResultSet;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;


public class DriverAdapter implements Iterable<Results.Row<Sample>>, Iterator<Results.Row<Sample>> {

    private final Iterator<com.datastax.driver.core.Row> m_results;
    private final Set<String> m_metrics;
    private Results.Row<Sample> m_next = null;

    public DriverAdapter(ResultSet input) {
        this(input, new ResultDescriptor());
    }

    /**
     * Construct a new {@link DriverAdapter}.
     * 
     * @param input
     *            cassandra driver {@link ResultSet}
     * @param metrics
     *            the set of result metrics to include; an empty set indicates that all metrics
     *            should be included
     */
    public DriverAdapter(ResultSet input, ResultDescriptor resultDescriptor) {
        checkNotNull(input, "input argument");
        checkNotNull(resultDescriptor, "result descriptor argument");

        m_results = input.iterator();
        m_metrics = getSourceNames(resultDescriptor);

        if (m_results.hasNext()) {
            Sample m = getSample(m_results.next());
            m_next = new Results.Row<Sample>(m.getTimestamp(), m.getResource());
            addSample(m_next, m);
        }

    }

    @Override
    public boolean hasNext() {
        return m_next != null;
    }

    @Override
    public Results.Row<Sample> next() {

        if (!hasNext()) throw new NoSuchElementException();

        Results.Row<Sample> nextNext = null;

        while (m_results.hasNext()) {
            Sample m = getSample(m_results.next());

            if (m.getTimestamp().gt(m_next.getTimestamp())) {
                nextNext = new Results.Row<Sample>(m.getTimestamp(), m.getResource());
                addSample(nextNext, m);
                break;
            }

            addSample(m_next, m);
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
    public Iterator<Row<Sample>> iterator() {
        return this;
    }

    private void addSample(Results.Row<Sample> row, Sample sample) {
        if (m_metrics.size() == 0 || m_metrics.contains(sample.getName())) {
            row.addElement(sample);
        }
    }

    private Sample getSample(com.datastax.driver.core.Row row) {
        MetricType type = getMetricType(row);
        return new Sample(getTimestamp(row), getResource(row), getMetricName(row), type, getValue(row, type));
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

    private static Set<String> getSourceNames(ResultDescriptor resultDescriptor) {
        final Function<Datasource, String> datasourceToSourceName = new Function<Datasource, String>() {
            @Override
            public String apply(Datasource input) {
                return input.getSource();
            }
        };

        return Sets.newHashSet(Iterables.transform(resultDescriptor.getDatasources().values(), datasourceToSourceName));
    }

}
