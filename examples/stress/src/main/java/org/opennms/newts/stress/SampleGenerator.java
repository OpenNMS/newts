package org.opennms.newts.stress;


import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Iterator;

import org.opennms.newts.aggregate.IntervalGenerator;
import org.opennms.newts.api.Duration;
import org.opennms.newts.api.Gauge;
import org.opennms.newts.api.MetricType;
import org.opennms.newts.api.Sample;
import org.opennms.newts.api.Timestamp;
import org.opennms.newts.api.ValueType;

import com.google.common.base.Optional;


/**
 * Generates samples for a given resource and metric. Yields {@link Optional}s as a means of
 * introducing missed samples.
 * 
 * @author eevans
 */
public class SampleGenerator implements Iterable<Optional<Sample>>, Iterator<Optional<Sample>> {

    private final IntervalGenerator m_intervals;
    private final String m_resource;
    private final String m_metric;

    public SampleGenerator(String resource, String metric, Timestamp start, Timestamp end, Duration interval) {
        m_resource = checkNotNull(resource, "resource argument");
        m_metric = checkNotNull(metric, "metric argument");

        checkNotNull(start, "start argument");
        checkNotNull(end, "end argument");
        checkNotNull(interval, "interval argument");
        m_intervals = new IntervalGenerator(start, end, interval);
    }

    @Override
    public boolean hasNext() {
        return m_intervals.hasNext();
    }

    @Override
    public Optional<Sample> next() {
        return Optional.of(new Sample(m_intervals.next(), m_resource, m_metric, MetricType.GAUGE, value()));
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<Optional<Sample>> iterator() {
        return this;
    }

    private ValueType<?> value() {
        return new Gauge(1.0d); // XXX: hard-coded for now
    }

}
