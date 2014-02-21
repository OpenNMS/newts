package org.opennms.newts.persistence.cassandra;


import static com.google.common.base.Preconditions.checkNotNull;
import static org.opennms.newts.api.MetricType.GAUGE;

import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

import org.opennms.newts.api.Sample;
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
    private final Map<String, Sample> m_prevSamples = Maps.newHashMap();

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
            Sample sample = working.getSample(metricName);

            if (sample != null) {
                result.addSample(getRate(sample));
                m_prevSamples.put(sample.getName(), sample);
            }
        }

        return result;
    }

    private Sample getRate(Sample sample) {
        ValueType<?> value = null;
        Sample previous = m_prevSamples.get(sample.getName());

        if (previous != null) {
            long elapsed = sample.getTimestamp().asSeconds() - previous.getTimestamp().asSeconds();
            value = sample.getValue().delta(previous.getValue()).divideBy(elapsed);
        }

        return new Sample(sample.getTimestamp(), sample.getResource(), sample.getName(), GAUGE, value);
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
