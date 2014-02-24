package org.opennms.newts.api.query;


import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import org.opennms.newts.api.Duration;

import com.google.common.base.Function;


public class Datasource {
    
    public static interface AggregationFunction extends Function<List<Double>, Double> {
        /**
         * An Aggregation function is a function that takes the list of PDPs should be aggregated in a single bucket
         * of resolution.  For example, if the step size if 5m and the resolution is 1h then the aggregation function
         * will be called with a list of 12 values.  These function should ignore all NaN values as if they were not
         * included in the list at all.
         */
        public Double apply(List<Double> input);
    };
    
    public static enum StandardAggregationFunctions implements AggregationFunction {
        
        // These function assume that the xff calculation is done elsewhere and that the values that 
        // are returned is non sensible if there are more NaN then defined by the xff.
        AVERAGE {

            @Override
            public Double apply(List<Double> input) {
                int count = 0;
                Double sum = 0.0d;
                for(Double item : input) {
                    if (!Double.isNaN(item)) {
                        sum += item;
                        count++;
                    }
                }
                
                return sum / count;
            }
            
        },
        MAX {

            @Override
            public Double apply(List<Double> input) {
                Double max = Double.MIN_VALUE;
                for(Double item : input) {
                    if (!Double.isNaN(item)) {
                        double diff = item - max;
                        max = diff > 0 ? item : max;
                    }
                }
                return max;
            }
            
        },
        MIN {

            @Override
            public Double apply(List<Double> input) {
                Double min = Double.MAX_VALUE;
                for(Double item : input) {
                    if (!Double.isNaN(item)) {
                        double diff = item - min;
                        min = diff < 0 ? item : min;
                    }
                }
                return min;
            }
            
        }
        
        
    }

    private final String m_label;
    private final String m_source;
    private final Duration m_heartbeat;
    private final double m_xff;
    private final AggregationFunction m_aggregationFunction;

    public Datasource(String label, String sourceName, Duration heartbeat, double xff, AggregationFunction aggregationFunction) {
        checkNotNull(label, "label argument");
        checkNotNull(sourceName, "source name argument");
        checkNotNull(heartbeat, "heartbeat argument");

        m_label = label;
        m_source = sourceName;
        m_heartbeat = heartbeat;
        m_xff = xff;
        m_aggregationFunction = aggregationFunction;
    }

    public String getLabel() {
        return m_label;
    }

    public String getSource() {
        return m_source;
    }

    public Duration getHeartbeat() {
        return m_heartbeat;
    }
    
    public double getXff() {
        return m_xff;
    }
    
    public AggregationFunction getAggregationFuction() {
        return m_aggregationFunction;
    }

}
