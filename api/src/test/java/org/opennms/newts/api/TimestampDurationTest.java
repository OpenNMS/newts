package org.opennms.newts.api;


import static org.junit.Assert.*;
import static org.opennms.newts.api.Timestamp.fromEpochSeconds;

import java.util.concurrent.TimeUnit;

import org.junit.Test;


public class TimestampDurationTest {

    @Test
    public void testDurationMultiplication() {
        assertEquals(Duration.seconds(25), Duration.seconds(5).times(5));
    }

    @Test
    public void testDurationDivideBy() {
        assertEquals(5, Duration.seconds(25).divideBy(Duration.seconds(5)));
    }

    @Test
    public void testDurationIsMultiple() {
        assertTrue(Duration.millis(50000).isMultiple(Duration.seconds(10)));
        assertFalse(Duration.seconds(5).isMultiple(Duration.seconds(2)));
    }

    @Test
    public void testTimestampFloor() {
       assertEquals(fromEpochSeconds(0), fromEpochSeconds(0).stepFloor(Duration.minutes(60)));
    }

    @Test
    public void testTimestampCeiling() {
        assertEquals(fromEpochSeconds(900), fromEpochSeconds(601).stepCeiling(Duration.seconds(300)));
        assertEquals(fromEpochSeconds(900), fromEpochSeconds(900).stepCeiling(Duration.seconds(300)));
        assertEquals(fromEpochSeconds(3600), fromEpochSeconds(3300).stepCeiling(Duration.minutes(60)));
    }

    @Test
    public void test() {
        assertTrue(Timestamp.isFiner(TimeUnit.MILLISECONDS, TimeUnit.SECONDS));
        assertFalse(Timestamp.isFiner(TimeUnit.SECONDS, TimeUnit.MILLISECONDS));
        assertFalse(Timestamp.isFiner(TimeUnit.MILLISECONDS, TimeUnit.MILLISECONDS));
        assertFalse(Timestamp.isFiner(TimeUnit.SECONDS, TimeUnit.SECONDS));

        Duration sec = Duration.seconds(1);
        Duration kMillis = Duration.millis(1000);
        Duration milli = Duration.millis(1);

        Timestamp before = new Timestamp(1, TimeUnit.SECONDS);

        Timestamp beforeMillis = new Timestamp(1000, TimeUnit.MILLISECONDS);

        Timestamp afterASec = before.plus(sec);

        assertEquals(afterASec.m_time, 2);
        assertEquals(afterASec.m_unit, TimeUnit.SECONDS);

        Timestamp after1kMillis = before.plus(kMillis);

        assertEquals(after1kMillis.m_time, 2000);
        assertEquals(after1kMillis.m_unit, TimeUnit.MILLISECONDS);

        Timestamp twoKMillis = beforeMillis.plus(sec);

        assertEquals(twoKMillis.m_time, 2000);
        assertEquals(twoKMillis.m_unit, TimeUnit.MILLISECONDS);

        Timestamp after1Milli = before.plus(milli);

        assertEquals(after1Milli.m_time, 1001);
        assertEquals(after1Milli.m_unit, TimeUnit.MILLISECONDS);
        
        // Duration comparison
        assertTrue(sec.equals(Duration.millis(1000)));
        assertTrue(Duration.millis(1000).equals(Duration.seconds(1)));
        assertTrue(sec.gt(milli));
        assertTrue(sec.gte(milli));
        assertTrue(milli.lt(sec));
        assertTrue(milli.lte(sec));
        assertTrue(milli.lte(Duration.millis(1)));
        assertTrue(milli.gte(Duration.millis(1)));
        assertEquals(kMillis.hashCode(), sec.hashCode());

    }

}
