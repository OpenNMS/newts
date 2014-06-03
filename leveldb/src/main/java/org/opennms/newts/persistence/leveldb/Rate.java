package org.opennms.newts.persistence.leveldb;


import static com.google.common.base.Preconditions.checkNotNull;
import static org.opennms.newts.api.MetricType.GAUGE;

import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.opennms.newts.api.Gauge;
import org.opennms.newts.api.MetricType;
import org.opennms.newts.api.Results.Row;
import org.opennms.newts.api.Sample;
import org.opennms.newts.api.ValueType;

import com.google.common.collect.Maps;


/**
 * Conditionally calculate rate (per-second) on samples.
 * 
 * @author eevans
 */
class Rate implements Iterator<Row<Sample>>, Iterable<Row<Sample>> {

    private static final Gauge NAN = new Gauge(Double.NaN);

    private final Iterator<Row<Sample>> m_input;
    private final Set<String> m_metrics;
    private final Map<String, Sample> m_prevSamples = Maps.newHashMap();

    Rate(Iterator<Row<Sample>> input, Set<String> metrics) {
        m_input = checkNotNull(input, "input argument");
        m_metrics = checkNotNull(metrics, "metrics argument");
    }

    @Override
    public boolean hasNext() {
        return m_input.hasNext();
    }

    @Override
    public Row<Sample> next() {

        if (!hasNext()) throw new NoSuchElementException();

        Row<Sample> working = m_input.next();
        Row<Sample> result = new Row<>(working.getTimestamp(), working.getResource());

        for (String metricName : m_metrics) {
            Sample sample = working.getElement(metricName);

            if (sample == null) {
                continue;
            }

            // Use rate as result if COUNTER, else pass through as-is.
            result.addElement(sample.getType().equals(MetricType.COUNTER) ? getRate(sample) : sample);

            m_prevSamples.put(sample.getName(), sample);

        }

        return result;
    }

    private Sample getRate(Sample sample) {

        ValueType<?> value = NAN;
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
    public Iterator<Row<Sample>> iterator() {
        return this;
    }

}
