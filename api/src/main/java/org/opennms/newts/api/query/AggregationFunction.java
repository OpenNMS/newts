package org.opennms.newts.api.query;


import java.util.Collection;

import com.google.common.base.Function;


public interface AggregationFunction extends Function<Collection<Double>, Double> {

    /**
     * An Aggregation function is a function that takes the list of PDPs should be aggregated in a
     * single bucket of resolution. For example, if the step size if 5m and the resolution is 1h
     * then the aggregation function will be called with a list of 12 values. These function should
     * ignore all NaN values as if they were not included in the list at all.
     */
    public Double apply(Collection<Double> input);

}
