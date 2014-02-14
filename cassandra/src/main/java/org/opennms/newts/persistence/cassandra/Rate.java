package org.opennms.newts.persistence.cassandra;


import static com.google.common.base.Preconditions.checkNotNull;
import static org.opennms.newts.api.MetricType.GAUGE;

import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

import org.opennms.newts.api.Measurement;
import org.opennms.newts.api.Results.Row;
import org.opennms.newts.api.ValueType;

import com.google.common.collect.Maps;


/**
 * Calculates rate (per-second).
 * 
 * @author eevans
 */
public class Rate implements Iterator<Row>, Iterable<Row> {

    private final Iterator<Row> m_input;
    private final String[] m_metrics;
    private final Map<String, Measurement> m_prevMeasurements = Maps.newHashMap();

    public Rate(Iterator<Row> input, String[] metrics) {
        m_input = checkNotNull(input, "input argument");
        m_metrics = checkNotNull(metrics, "metrics argument");
    }

    @Override
    public boolean hasNext() {
        return m_input.hasNext();
    }

    @Override
    public Row next() {

        if (!hasNext()) throw new NoSuchElementException();

        Row working = m_input.next();
        Row result = new Row(working.getTimestamp(), working.getResource());

        for (String metricName : m_metrics) {
            Measurement measurement = working.getMeasurement(metricName);

            if (measurement != null) {
                result.addMeasurement(getRate(measurement));
                m_prevMeasurements.put(measurement.getName(), measurement);
            }
        }

        return result;
    }

    private Measurement getRate(Measurement measurement) {
        ValueType<?> value = null;
        Measurement previous = m_prevMeasurements.get(measurement.getName());

        if (previous != null) {
            long elapsed = measurement.getTimestamp().asSeconds() - previous.getTimestamp().asSeconds();
            value = measurement.getValue().delta(previous.getValue()).divideBy(elapsed);
        }

        return new Measurement(measurement.getTimestamp(), measurement.getResource(), measurement.getName(), GAUGE, value);
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<Row> iterator() {
        return this;
    }

}
