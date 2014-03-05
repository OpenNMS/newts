package org.opennms.newts.api.query;


import java.util.Collection;


public enum StandardAggregationFunctions implements AggregationFunction {

    // These function assume that the xff calculation is done elsewhere and that the values that
    // are returned is non sensible if there are more NaN then defined by the xff.
    AVERAGE {

        @Override
        public Double apply(Collection<Double> input) {
            int count = 0;
            Double sum = 0.0d;
            for (Double item : input) {
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
        public Double apply(Collection<Double> input) {
            Double max = Double.MIN_VALUE;
            for (Double item : input) {
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
        public Double apply(Collection<Double> input) {
            Double min = Double.MAX_VALUE;
            for (Double item : input) {
                if (!Double.isNaN(item)) {
                    double diff = item - min;
                    min = diff < 0 ? item : min;
                }
            }
            return min;
        }

    }

}
