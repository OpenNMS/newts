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
