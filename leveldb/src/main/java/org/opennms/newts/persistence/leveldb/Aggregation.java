package org.opennms.newts.persistence.leveldb;


import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.opennms.newts.api.Duration;
import org.opennms.newts.api.Measurement;
import org.opennms.newts.api.Results.Row;
import org.opennms.newts.api.Timestamp;
import org.opennms.newts.api.query.Datasource;
import org.opennms.newts.api.query.ResultDescriptor;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;


/**
 * Apply aggregate functions to measurements.
 *
 * @author eevans
 */
class Aggregation implements Iterable<Row<Measurement>>, Iterator<Row<Measurement>> {

    private final ResultDescriptor m_resultDescriptor;
    private final String m_resource;
    private final Iterator<Timestamp> m_timestamps;
    private final Duration m_resolution;
    private final Iterator<Row<Measurement>> m_input;
    private final double m_intervalsPer;

    private Row<Measurement> m_working;
    private Row<Measurement> m_nextOut;

    Aggregation(String resource, Timestamp start, Timestamp end, ResultDescriptor resultDescriptor, Duration resolution, Iterator<Row<Measurement>> input) {
        m_resultDescriptor = checkNotNull(resultDescriptor, "result descriptor argument");
        m_resource = checkNotNull(resource, "resource argument");
        checkNotNull(start, "start argument");
        checkNotNull(end, "end argument");
        m_resolution = checkNotNull(resolution, "resolution argument");
        m_input = checkNotNull(input, "input argument");

        Duration interval = resultDescriptor.getInterval();
        checkArgument(resolution.isMultiple(interval), "resolution must be a multiple of interval");

        m_timestamps = new IntervalGenerator(start, end, m_resolution);
        m_intervalsPer = (double) resolution.divideBy(interval);

        m_working = m_input.hasNext() ? m_input.next() : null;
        m_nextOut = m_timestamps.hasNext() ? new Row<Measurement>(m_timestamps.next(), m_resource) : null;

        // If the input stream contains any Samples earlier than what's relevant, iterate past them.
        if (m_nextOut != null) {
            while (m_working != null && m_working.getTimestamp().lte(m_nextOut.getTimestamp().minus(m_resolution))) {
                m_working = nextWorking();
            }
        }

    }

    @Override
    public boolean hasNext() {
        return m_nextOut != null;
    }

    @Override
    public Row<Measurement> next() {

        if (!hasNext()) throw new NoSuchElementException();

        Multimap<String, Double> values = ArrayListMultimap.create();

        while (inRange()) {
            // accumulate
            for (Datasource ds : getDatasources()) {
                Measurement m = m_working.getElement(ds.getSource());
                values.put(ds.getLabel(), m != null ? m.getValue() : Double.NaN);
            }

            m_working = nextWorking();
        }

        for (Datasource ds : getDatasources()) {
            Double v = aggregate(ds, values.get(ds.getLabel()));
            m_nextOut.addElement(new Measurement(m_nextOut.getTimestamp(), m_resource, ds.getLabel(), v));
        }

        try {
            return m_nextOut;
        }
        finally {
            m_nextOut = m_timestamps.hasNext() ? new Row<Measurement>(m_timestamps.next(), m_resource) : null;
        }
    }

    // Return the result of this Datasource's aggregation function if the number of values
    // is within XFF, otherwise return NaN.
    private Double aggregate(Datasource ds, Collection<Double> values) {
        return ((values.size() / m_intervalsPer) > ds.getXff()) ? ds.getAggregationFuction().apply(values) : Double.NaN;
    }

    // true if the working input Row is within the Range of the next output Row; false otherwise
    private boolean inRange() {
        if (m_working == null || m_nextOut == null) {
            return false;
        }

        Timestamp rangeUpper = m_nextOut.getTimestamp();
        Timestamp rangeLower = m_nextOut.getTimestamp().minus(m_resolution);

        return m_working.getTimestamp().lte(rangeUpper) && m_working.getTimestamp().gt(rangeLower);
    }

    private Row<Measurement> nextWorking() {
        return m_input.hasNext() ? m_input.next() : null;
    }

    private Collection<Datasource> getDatasources() {
        return m_resultDescriptor.getDatasources().values();
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
