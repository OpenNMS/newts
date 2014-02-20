package org.opennms.newts.api.query;


import java.util.List;

import org.opennms.newts.api.Duration;
import org.opennms.newts.api.Gauge;
import org.opennms.newts.api.ValueType;

import com.google.common.base.Function;

import static com.google.common.base.Preconditions.checkNotNull;


public class Datasource {
    
    public static interface AggregationFunction extends Function<List<ValueType<Double>>, ValueType<Double>> {
        /**
         * An Aggregation function is a function that takes the list of PDPs should be aggregated in a single bucket
         * of resolution.  For example, if the step size if 5m and the resolution is 1h then the aggregation function
         * will be called with a list of 12 values.  These function should ignore all NaN values as if they were not
         * included in the list at all.
         */
        public ValueType<Double> apply(List<ValueType<Double>> input);
    };
    
    public static enum StandardAggregationFunctions implements AggregationFunction {
        
        // These function assume that the xff calculation is done elsewhere and that the values that 
        // are returned is non sensible if there are more NaN then defined by the xff.
        AVERAGE {

            @Override
            public ValueType<Double> apply(List<ValueType<Double>> input) {
                int count = 0;
                ValueType<Double> sum = new Gauge(0);
                for(ValueType<Double> item : input) {
                    if (!item.isNan()) {
                        sum = sum.plus(item);
                        count++;
                    }
                }
                
                return sum.divideBy(count);
            }
            
        },
        MAX {

            @Override
            public ValueType<Double> apply(List<ValueType<Double>> input) {
                ValueType<Double> max = new Gauge(Double.MIN_VALUE);
                for(ValueType<Double> item : input) {
                    if (!item.isNan()) {
                        double diff = item.minus(max).doubleValue();
                        max = diff > 0 ? item : max;
                    }
                }
                return max;
            }
            
        },
        MIN {

            @Override
            public ValueType<Double> apply(List<ValueType<Double>> input) {
                ValueType<Double> min = new Gauge(Double.MAX_VALUE);
                for(ValueType<Double> item : input) {
                    if (!item.isNan()) {
                        double diff = item.minus(min).doubleValue();
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
