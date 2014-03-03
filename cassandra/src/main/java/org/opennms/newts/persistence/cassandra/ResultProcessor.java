package org.opennms.newts.persistence.cassandra;


import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Iterator;

import org.opennms.newts.api.Duration;
import org.opennms.newts.api.Measurement;
import org.opennms.newts.api.Results;
import org.opennms.newts.api.Results.Row;
import org.opennms.newts.api.Sample;
import org.opennms.newts.api.Timestamp;
import org.opennms.newts.api.query.ResultDescriptor;

// Encapsulation of results processing.

public class ResultProcessor {

    private final String m_resource;
    private final Timestamp m_start;
    private final Timestamp m_end;
    private final ResultDescriptor m_resultDescriptor;
    private final Duration m_resolution;

    public ResultProcessor(String resource, Timestamp start, Timestamp end, ResultDescriptor descriptor, Duration resolution) {
        m_resource = checkNotNull(resource, "resource argument");
        m_start = checkNotNull(start, "start argument");
        m_end = checkNotNull(end, "end argument");
        m_resultDescriptor = checkNotNull(descriptor, "result descriptor argument");
        m_resolution = checkNotNull(resolution, "resolution argument");
    }

    public Results<Measurement> process(Iterator<Row<Sample>> samples) {
        checkNotNull(samples, "samples argument");

        // Build chain of iterators to process results as a stream
        Rate rate = new Rate(samples, m_resultDescriptor.getSourceNames());
        PrimaryData primaryData = new PrimaryData(m_resource, m_start, m_end, m_resultDescriptor, rate);
        Aggregation aggregation = new Aggregation(m_resource, m_start, m_end, m_resultDescriptor, m_resolution, primaryData);
        Export exports = new Export(m_resultDescriptor.getExports(), aggregation);

        Results<Measurement> measurements = new Results<Measurement>();

        for (Row<Measurement> row : exports) {
            measurements.addRow(row);
        }

        return measurements;
    }

}
