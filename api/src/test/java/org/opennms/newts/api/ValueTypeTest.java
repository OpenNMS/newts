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
package org.opennms.newts.api;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.google.common.primitives.UnsignedLong;


public class ValueTypeTest {

    @Test
    public void testAbsoluteFromNumber() {
        assertTrue(ValueType.compose(100, MetricType.ABSOLUTE) instanceof Absolute);
    }

    @Test
    public void testCounterFromNumber() {
        assertTrue(ValueType.compose(100, MetricType.COUNTER) instanceof Counter);
    }

    @Test
    public void testDeriveFromNumber() {
        assertTrue(ValueType.compose(100, MetricType.DERIVE) instanceof Derive);
    }

    @Test
    public void testGaugeFromNumber() {
        assertTrue(ValueType.compose(100, MetricType.GAUGE) instanceof Gauge);
    }

    @Test
    public void testAbsoluteSerialization() {
        Absolute c0 = new Absolute(UnsignedLong.fromLongBits(50L));
        ValueType<?> c1 = ValueType.compose(ValueType.decompose(c0));
        assertTrue(c1 instanceof Absolute);
        assertEquals(c0, c1);
    }

    @Test
    public void testCounterSerialization() {
        Counter c0 = new Counter(UnsignedLong.fromLongBits(50L));
        ValueType<?> c1 = ValueType.compose(ValueType.decompose(c0));
        assertTrue(c1 instanceof Counter);
        assertEquals(c0, c1);
    }

    @Test
    public void testDeriveSerialization() {
        Derive c0 = new Derive(UnsignedLong.fromLongBits(50L));
        ValueType<?> c1 = ValueType.compose(ValueType.decompose(c0));
        assertTrue(c1 instanceof Derive);
        assertEquals(c0, c1);
    }

    @Test
    public void testGaugeSerialization() {
        Gauge c0 = new Gauge(3.1416d);
        ValueType<?> c1 = ValueType.compose(ValueType.decompose(c0));
        assertTrue(c1 instanceof Gauge);
        assertEquals(c0, c1);
    }

}
