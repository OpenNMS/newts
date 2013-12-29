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
        ValueType<?> c1 = ValueType.compose(ValueType.decompose(c0), c0.getType());
        assertTrue(c1 instanceof Absolute);
        assertEquals(c0, c1);
    }

    @Test
    public void testCounterSerialization() {
        Counter c0 = new Counter(UnsignedLong.fromLongBits(50L));
        ValueType<?> c1 = ValueType.compose(ValueType.decompose(c0), c0.getType());
        assertTrue(c1 instanceof Counter);
        assertEquals(c0, c1);
    }

    @Test
    public void testDeriveSerialization() {
        Derive c0 = new Derive(UnsignedLong.fromLongBits(50L));
        ValueType<?> c1 = ValueType.compose(ValueType.decompose(c0), c0.getType());
        assertTrue(c1 instanceof Derive);
        assertEquals(c0, c1);
    }

    @Test
    public void testGaugeSerialization() {
        Gauge c0 = new Gauge(3.1416d);
        ValueType<?> c1 = ValueType.compose(ValueType.decompose(c0), c0.getType());
        assertTrue(c1 instanceof Gauge);
        assertEquals(c0, c1);
    }

}
