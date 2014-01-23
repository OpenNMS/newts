package org.opennms.newts.api;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.opennms.newts.api.Aggregates.Point;

import com.google.common.collect.Lists;
import com.google.common.primitives.UnsignedLong;


public class AggregatesTest {

    @Test
    public void testRollup() {

        List<Point> pointsIn = Lists.newArrayList();

        pointsIn.add(new Point(fromSeconds(99), new Gauge(2.0d)));
        pointsIn.add(new Point(fromSeconds(199), new Gauge(3.0d)));
        pointsIn.add(new Point(fromSeconds(299), new Gauge(4.0d)));
        pointsIn.add(new Point(fromSeconds(399), new Gauge(3.0d)));
        pointsIn.add(new Point(fromSeconds(499), new Gauge(4.0d)));
        pointsIn.add(new Point(fromSeconds(599), new Gauge(5.0d)));

        Collection<Point> pointsOut = Aggregates.rollup(fromSeconds(0), fromSeconds(305), 300, TimeUnit.SECONDS, pointsIn);

        System.err.println(pointsOut);

    }

    @Test
    public void testRateCalc() {

        List<Point> pointsIn = Lists.newArrayList();

        for (int i = 0; i < 10; i++) {
            pointsIn.add(new Point(fromSeconds(i * 10), new Counter(UnsignedLong.fromLongBits(i * 100))));
        }

        Collection<Point> pointsOut = Aggregates.rate(pointsIn);

        assertEquals(pointsIn.size(), pointsOut.size());

        Iterator<Point> pointsIter = pointsOut.iterator();

        assertNull(pointsIter.next());

        while (pointsIter.hasNext()) {
            assertEquals(10, pointsIter.next().y.longValue());
        }

    }

    @Test
    public void testTimestampIterator() {

        List<Timestamp> timestamps = Lists.newArrayList(getTimestamps(150, 3500));

        assertEquals(12, timestamps.size());
        assertEquals(new Timestamp(300, TimeUnit.SECONDS), timestamps.get(0));
        assertEquals(new Timestamp(3600, TimeUnit.SECONDS), timestamps.get(11));

        timestamps = Lists.newArrayList(getTimestamps(0, 3600));

        assertEquals(13, timestamps.size());
        assertEquals(new Timestamp(300, TimeUnit.SECONDS), timestamps.get(0));
        assertEquals(new Timestamp(3900, TimeUnit.SECONDS), timestamps.get(12));

    }

    private Iterable<Timestamp> getTimestamps(long startSecs, long endSecs) {
        return new Aggregates.Timestamps(fromSeconds(startSecs), fromSeconds(endSecs));
    }

    private Timestamp fromSeconds(long value) {
        return new Timestamp(value, TimeUnit.SECONDS);
    }

}
