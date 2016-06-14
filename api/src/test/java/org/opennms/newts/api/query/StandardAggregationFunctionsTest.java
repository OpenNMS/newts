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


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.opennms.newts.api.query.StandardAggregationFunctions.AVERAGE;
import static org.opennms.newts.api.query.StandardAggregationFunctions.MAX;
import static org.opennms.newts.api.query.StandardAggregationFunctions.MIN;
import static org.opennms.newts.api.query.StandardAggregationFunctions.P95;
import static org.opennms.newts.api.query.StandardAggregationFunctions.P99;

import org.junit.Test;

import com.google.common.collect.Sets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class StandardAggregationFunctionsTest {

    @Test
    public void testMaxEmpty() {
        Double result = MAX.apply(new ArrayList<Double>());
        assertEquals(Double.MIN_VALUE, (double) result, 0.0);
    }

    @Test
    public void testMaxOne() {
        Double result = MAX.apply(Arrays.asList(42.0));
        assertEquals(42.0, (double) result, 0.0);
    }

    @Test
    public void testMaxMany() {
        Double result = MAX.apply(Arrays.asList(41.0, 42.0, 43.0));
        assertEquals(43.0, (double) result, 0.0);
    }

    @Test
    public void testMinEmpty() {
        Double result = MIN.apply(new ArrayList<Double>());
        assertEquals(Double.MAX_VALUE, (double) result, 0.0);
    }

    @Test
    public void testMinOne() {
        Double result = MIN.apply(Arrays.asList(42.0));
        assertEquals(42.0, (double) result, 0.0);
    }

    @Test
    public void testMinMany() {
        Double result = MIN.apply(Arrays.asList(41.0, 42.0, 43.0));
        assertEquals(41.0, (double) result, 0.0);
    }

    @Test
    public void testAverageEmpty() {
        Double result = AVERAGE.apply(new ArrayList<Double>());
        assertEquals(Double.NaN, (double) result, 0.0);
    }

    @Test
    public void testAverageOne() {
        Double result = AVERAGE.apply(Arrays.asList(42.0));
        assertEquals(42.0, (double) result, 0.0);
    }

    @Test
    public void testAverageMany() {
        Double result = AVERAGE.apply(Arrays.asList(41.0, 42.0, 43.0));
        assertEquals(42.0, (double) result, 0.0);
    }

    @Test
    public void testP95Empty() {
        Double result = P95.apply(new ArrayList<Double>());
        assertEquals(Double.MAX_VALUE, (double) result, 0.0);
    }

    @Test
    public void testP95One() {
        Double result = P95.apply(Arrays.asList(42.0));
        assertEquals(42.0, (double) result, 0.0);
    }

    @Test
    public void testP95Many() {
        List<Double> values = new ArrayList<Double>();
        for (int i = 0; i < 100; i++) {
            values.add((double) i);
        }
        Double result = P95.apply(values);
        assertEquals(95.0, (double) result, 0.0);
    }

    @Test
    public void testP99Empty() {
        Double result = P99.apply(new ArrayList<Double>());
        assertEquals(Double.MAX_VALUE, (double) result, 0.0);
    }

    @Test
    public void testP99One() {
        Double result = P99.apply(Arrays.asList(42.0));
        assertEquals(42.0, (double) result, 0.0);
    }

    @Test
    public void testP99Many() {
        List<Double> values = new ArrayList<Double>();
        for (int i = 0; i < 100; i++) {
            values.add((double) i);
        }
        Double result = P99.apply(values);
        assertEquals(99.0, (double) result, 0.0);
    }
}
