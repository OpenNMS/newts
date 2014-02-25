package org.opennms.newts.persistence.cassandra;


import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

import org.opennms.newts.api.Duration;
import org.opennms.newts.api.Measurement;
import org.opennms.newts.api.Sample;
import org.opennms.newts.api.MetricType;
import org.opennms.newts.api.Results.Row;
import org.opennms.newts.api.Timestamp;
import org.opennms.newts.api.ValueType;
import org.opennms.newts.api.query.Datasource;
import org.opennms.newts.api.query.ResultDescriptor;

import com.google.common.collect.Maps;


public class PrimaryData implements Iterator<Row<Measurement>>, Iterable<Row<Measurement>> {

    private class Accumulation {
        private long known, unknown;
        private ValueType<?> value;

        private Accumulation() {
            reset();
        }

        private double elapsed() {
            return known + unknown;
        }

        private Double average() {
            return isValid() ? value.divideBy(known).doubleValue() : Double.NaN;
        }

        private boolean isValid() {
            return unknown < (elapsed() / 2);
        }

        private void reset() {
            known = unknown = 0;
            value = ValueType.compose(0, MetricType.GAUGE);
        }

    }

    private final ResultDescriptor m_resultDescriptor;
    private final String m_resource;
    private final Iterator<Timestamp> m_timestamps;
    private final Duration m_interval;
    private final Iterator<Row<Sample>> m_input;
    private final Map<String, Sample> m_lastUpdates = Maps.newHashMap();
    private final Map<String, Accumulation> m_accumulation = Maps.newHashMap();

    private Row<Sample> m_current = null;

    public PrimaryData(ResultDescriptor resultDescriptor, String resource, Timestamp start, Timestamp end, Iterator<Row<Sample>> input) {
        m_resultDescriptor = checkNotNull(resultDescriptor, "result descriptor argument");
        m_resource = checkNotNull(resource, "resource argument");
        checkNotNull(start, "start argument");
        checkNotNull(end, "end argument");
        m_input = checkNotNull(input, "input argument");

        m_interval = resultDescriptor.getStep();

        m_timestamps = new IntervalGenerator(start, end, m_interval);

        if (m_input.hasNext()) m_current = m_input.next();

    }

    @Override
    public boolean hasNext() {
        return m_timestamps.hasNext();
    }

    @Override
    public Row<Measurement> next() {

        if (!hasNext()) throw new NoSuchElementException();

        Row<Measurement> output = new Row<>(m_timestamps.next(), m_resource);

        while (m_current != null) {
            accumulate(m_current, output.getTimestamp());

            if (m_current.getTimestamp().gte(output.getTimestamp())) {
                break;
            }

            if (m_input.hasNext()) {
                m_current = m_input.next();
            }
            else m_current = null;

        }

        // Go time; We've accumulated enough to produce the output row
        for (Datasource ds : m_resultDescriptor.getDatasources().values()) {

            Accumulation accumulation = m_accumulation.get(ds.getSource());

            // Add sample with accumulated value to output row
            output.addElement(new Measurement(
                    output.getTimestamp(),
                    output.getResource(),
                    ds.getSource(),
                    accumulation.average()));

            // If input is greater than row, accumulate remainder for next row
            if (m_current != null) {
                accumulation.reset();

                Sample sample = m_current.getElement(ds.getSource());

                if (sample == null) {
                    continue;
                }

                if (m_current.getTimestamp().gt(output.getTimestamp())) {
                    Duration elapsed = m_current.getTimestamp().minus(output.getTimestamp());
                    if (elapsed.lt(ds.getHeartbeat())) {
                        accumulation.known = elapsed.asMillis();
                        accumulation.value = sample.getValue().times(elapsed.asMillis());
                    }
                    else {
                        accumulation.unknown = elapsed.asMillis();
                    }
                }

            }
        }

        return output;
    }

    private void accumulate(Row<Sample> row, Timestamp intervalCeiling) {

        for (Datasource ds : m_resultDescriptor.getDatasources().values()) {
            Sample current, last;

            current = row.getElement(ds.getSource());

            if (current == null) {
                continue;
            }

            last = m_lastUpdates.get(current.getName());

            if (last == null) {
                m_lastUpdates.put(current.getName(), current);
                continue;
            }

            Duration elapsed;

            if (row.getTimestamp().gt(intervalCeiling)) {
                elapsed = intervalCeiling.minus(last.getTimestamp());
            }
            else {
                elapsed = current.getTimestamp().minus(last.getTimestamp());
            }

            Accumulation accumulation = getOrCreateAccumulation(current.getName());

            if (elapsed.lt(getHeartbeat(current.getName()))) {
                accumulation.known += elapsed.asMillis();
                accumulation.value = accumulation.value.plus(current.getValue().times(elapsed.asMillis()));
            }
            else {
                accumulation.unknown += elapsed.asMillis();
            }

            // Postpone storing as lastUpdate, we'll need this sample again...
            if (!current.getTimestamp().gt(intervalCeiling.plus(m_interval))) {
                m_lastUpdates.put(current.getName(), current);
            }
        }
    }

    public Accumulation getOrCreateAccumulation(String name) {
        Accumulation result = m_accumulation.get(name);

        if (result == null) {
            result = new Accumulation();
            m_accumulation.put(name, result);
        }

        return result;
    }

    /*
     * FIXME: If a multiple of interval is a reasonable way of providing a default (is using a
     * default is even reasonable at all?), then do something better than the hard-coded value here.
     */
    private Duration getHeartbeat(String metricName) {
        return m_resultDescriptor.getDatasources().get(metricName).getHeartbeat();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<Row<Measurement>> iterator() {
        return this;
    }

}
