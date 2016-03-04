/*
 * Copyright 2014, The OpenNMS Group
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.opennms.newts.api.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Collections;


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

    },
    P95 {

        @Override
        public Double apply(Collection<Double> input) {
            List<Double> sorted = new ArrayList<Double>();
            for (Double item : input) {
                if (!Double.isNaN(item)) {
                    sorted.add(item);
                }
            }
            if (sorted.isEmpty()) {
                return Double.MAX_VALUE;
            }
            Collections.sort(sorted);
            int p95Idx = (int) (0.95 * sorted.size());
            return sorted.get(p95Idx);
        }

    },
    P99 {

        @Override
        public Double apply(Collection<Double> input) {
            List<Double> sorted = new ArrayList<Double>();
            for (Double item : input) {
                if (!Double.isNaN(item)) {
                    sorted.add(item);
                }
            }
            if (sorted.isEmpty()) {
                return Double.MAX_VALUE;
            }
            Collections.sort(sorted);
            int p99Idx = (int) (0.99 * sorted.size());
            return sorted.get(p99Idx);
        }

    },


}
