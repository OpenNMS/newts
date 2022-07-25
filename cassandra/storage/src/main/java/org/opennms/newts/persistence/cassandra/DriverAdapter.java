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


import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.opennms.newts.api.Resource;
import org.opennms.newts.api.Results;
import org.opennms.newts.api.Results.Row;
import org.opennms.newts.api.Sample;
import org.opennms.newts.api.Timestamp;
import org.opennms.newts.api.ValueType;

/**
 * Map database results to Newts {@link Sample}s.
 *
 * @author eevans
 */
class DriverAdapter implements Iterable<Results.Row<Sample>>, Iterator<Results.Row<Sample>> {

    private final Iterator<com.datastax.oss.driver.api.core.cql.Row> m_results;
    private final Set<String> m_metrics;
    private Results.Row<Sample> m_next = null;
    private int m_count = 0;

    DriverAdapter(Iterator<com.datastax.oss.driver.api.core.cql.Row> input) {
        this(input, Collections.<String> emptySet());
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
    DriverAdapter(Iterator<com.datastax.oss.driver.api.core.cql.Row> input, Set<String> metrics) {
        m_results = checkNotNull(input, "input argument");
        m_metrics = checkNotNull(metrics, "metrics argument");

        if (m_results.hasNext()) {
            Sample m = getNextSample();
            m_next = new Results.Row<>(m.getTimestamp(), m.getResource());
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
            Sample m = getNextSample();

            if (m.getTimestamp().gt(m_next.getTimestamp())) {
                nextNext = new Results.Row<>(m.getTimestamp(), m.getResource());
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

    public int getResultCount() {
        return m_count;
    }

    private void addSample(Results.Row<Sample> row, Sample sample) {
        if (m_metrics.isEmpty() || m_metrics.contains(sample.getName())) {
            row.addElement(sample);
        }
    }

    private Sample getNextSample() {
        m_count += 1;
        return getSample(m_results.next());
    }

    private static Sample getSample(com.datastax.oss.driver.api.core.cql.Row row) {
        ValueType<?> value = getValue(row);
        return new Sample(getTimestamp(row), getResource(row), getMetricName(row), value.getType(), value, getAttributes(row));
    }

    private static ValueType<?> getValue(com.datastax.oss.driver.api.core.cql.Row row) {
        return ValueType.compose(row.getByteBuffer(SchemaConstants.F_VALUE));
    }

    private static String getMetricName(com.datastax.oss.driver.api.core.cql.Row row) {
        return row.getString(SchemaConstants.F_METRIC_NAME);
    }

    private static Timestamp getTimestamp(com.datastax.oss.driver.api.core.cql.Row row) {
        return Timestamp.fromEpochMillis(row.getInstant(SchemaConstants.F_COLLECTED).toEpochMilli());
    }

    private static Resource getResource(com.datastax.oss.driver.api.core.cql.Row row) {
        return new Resource(row.getString(SchemaConstants.F_RESOURCE));
    }

    private static Map<String, String> getAttributes(com.datastax.oss.driver.api.core.cql.Row row) {
        return row.getMap(SchemaConstants.F_ATTRIBUTES, String.class, String.class);
    }

}
