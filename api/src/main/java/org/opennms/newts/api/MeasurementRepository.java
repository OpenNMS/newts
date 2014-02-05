package org.opennms.newts.api;


import java.util.Collection;

import com.google.common.base.Optional;


public interface MeasurementRepository {

    /**
     * Read stored measurements.
     * 
     * @param resource
     *            name of the measured resource
     * @param start
     *            query start time (defaults to 24 hours less than {@code end}, if absent)
     * @param end
     *            query end time (defaults to current time if absent)
     * @param aggregates
     *            consolidation descriptor
     * @return query results
     */
    public Results select(String resource, Optional<Timestamp> start, Optional<Timestamp> end, Aggregates aggregates);

    /**
     * Read stored measurements.
     * 
     * @param resource
     *            name of the measured resource
     * @param start
     *            query start time (defaults to 24 hours less than {@code end}, if absent)
     * @param end
     *            query end time (defaults to current time if absent)
     * @return query results
     */
    public Results select(String resource, Optional<Timestamp> start, Optional<Timestamp> end);

    /**
     * Write (store) measurements.
     * 
     * @param measurements
     */
    public void insert(Collection<Measurement> measurements);
}
