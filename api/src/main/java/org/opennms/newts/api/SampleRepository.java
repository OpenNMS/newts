package org.opennms.newts.api;


import java.util.Collection;

import org.opennms.newts.api.query.ResultDescriptor;

import com.google.common.base.Optional;


public interface SampleRepository {

    /**
     * Query measurements.
     * 
     * @param resource
     *            name of the sampled resource
     * @param start
     *            query start time (defaults to 24 hours less than {@code end}, if absent)
     * @param end
     *            query end time (defaults to current time if absent)
     * @param descriptor
     *            aggregation descriptor
     * @param resolultion
     *            temporal resolution of results
     * @return query results
     */
    public Results<Measurement> select(String resource, Optional<Timestamp> start, Optional<Timestamp> end, ResultDescriptor descriptor, Duration resolution);

    /**
     * Read stored samples.
     * 
     * @param resource
     *            name of the sampled resource
     * @param start
     *            query start time (defaults to 24 hours less than {@code end}, if absent)
     * @param end
     *            query end time (defaults to current time if absent)
     * @return query results
     */
    public Results<Sample> select(String resource, Optional<Timestamp> start, Optional<Timestamp> end);

    /**
     * Write (store) samples.
     * 
     * @param samples
     */
    public void insert(Collection<Sample> samples);

}
