package org.opennms.newts.api;


import static java.lang.Double.NaN;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.opennms.newts.api.Duration.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.jrobin.core.ArcDef;
import org.jrobin.core.DsDef;
import org.jrobin.core.FetchData;
import org.jrobin.core.RrdDb;
import org.jrobin.core.RrdDef;
import org.jrobin.core.RrdException;
import org.jrobin.core.Sample;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.opennms.newts.api.AggregateFunctions.Point;

import com.google.common.collect.Lists;
import com.google.common.primitives.UnsignedLong;


public class AggregateFunctionsTest {

    private static final long DEFAULT_STEP_SECONDS = 300;
    private static final long DEFAULT_HEARTBEAT_SECONDS = 600;
    private static final double DEFAULT_XFF = 0.5d;

    private RrdDb m_gaugeRRD;

    private String rrdName() {
        return String.format("target/%s", UUID.randomUUID());
    }

    @Before
    public void setUp() throws RrdException, IOException {

        RrdDef def = new RrdDef(rrdName());

        def.setStep(DEFAULT_STEP_SECONDS);
        def.setStartTime(1422397609);
        def.addDatasource(new DsDef("name", "GAUGE", DEFAULT_HEARTBEAT_SECONDS, NaN, NaN));
        def.addArchive(new ArcDef("AVERAGE", DEFAULT_XFF, 1, 2016));

        m_gaugeRRD = new RrdDb(def);

    }

    @After
    public void tearDown() throws IOException {
        m_gaugeRRD.close();
    }

    @Test
    public void testRollup() throws RrdException, IOException {

//        Timestamp base = Timestamp.now();
        Timestamp base = new Timestamp(1422397610, TimeUnit.SECONDS);
        Timestamp start = base.minus(1, TimeUnit.SECONDS);

        Point[] points = new Point[] {
                new Point(base, new Gauge(2.0d)),
                new Point(base.plus(100, TimeUnit.SECONDS), new Gauge(3.0d)),
                new Point(base.plus(200, TimeUnit.SECONDS), new Gauge(4.0d)),
                new Point(base.plus(300, TimeUnit.SECONDS), new Gauge(5.0d)),
                new Point(base.plus(400, TimeUnit.SECONDS), new Gauge(6.0d)),
                new Point(base.plus(500, TimeUnit.SECONDS), new Gauge(7.0d)),
                new Point(base.plus(600, TimeUnit.SECONDS), new Gauge(8.0d)),
                new Point(base.plus(700, TimeUnit.SECONDS), new Gauge(9.0d)),
                new Point(base.plus(800, TimeUnit.SECONDS), new Gauge(10.0d)),
                new Point(base.plus(900, TimeUnit.SECONDS), new Gauge(11.0d)),
                new Point(base.plus(1000, TimeUnit.SECONDS), new Gauge(12.0d))
        };

        for (Point point : points)
            System.err.println(point.x.asSeconds() + ":  " + point.y.doubleValue());

        // RRD ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        Sample sample = m_gaugeRRD.createSample();

        for (Point point : points)
            sample.setAndUpdate(String.format("%d:%f", point.x.asSeconds(), point.y.doubleValue()));

        // long fromSecs = start.asSeconds() + 305;
        long fromSecs = start.asSeconds() + 0;
        FetchData data = m_gaugeRRD.createFetchRequest("AVERAGE", fromSecs, fromSecs + 600).fetchData();

        System.err.println();
        System.err.println(" --- JROBIN");
        System.err.println(data.dump());
        System.err.println();

        // Newts ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        Timestamp fromTimestamp = new Timestamp(fromSecs, TimeUnit.SECONDS);
        List<Point> pointsIn = Lists.newArrayList(points);
        Iterable<Point> pointsOut = AggregateFunctions.average(
                fromTimestamp,
                fromTimestamp.plus(Duration.seconds(600)),
                Duration.seconds(300),
                pointsIn);

        System.err.println(" --- Newts");
        for (Point point : pointsOut)
            System.err.println(point.x.asSeconds() + ":  " + ((point.y == null) ? "NaN" : point.y.doubleValue()));
        System.err.println();
        
        
        Collection<Point> mbPointsOut = AggregateFunctions.rollup2(fromTimestamp, fromTimestamp.plus(seconds(600)), seconds(300), seconds(600), Arrays.asList(points));

        System.err.println(" --- MB");
        for (Point point : mbPointsOut)
            System.err.println(point.x.asSeconds() + ":  " + ((point.y == null) ? "NaN" : point.y.doubleValue()));
        System.err.println();

        System.err.println("start=" + start.asSeconds() + ", end=" + start.plus(Duration.seconds(600)).asSeconds());
        System.err.println("start=" + start.asMillis()  + ", end=" + start.plus(Duration.seconds(600)).asMillis());

    }

    @Test
    public void testRateCalc() {

        List<Point> pointsIn = Lists.newArrayList();

        for (int i = 0; i < 10; i++) {
            pointsIn.add(new Point(fromSeconds(i * 10), new Counter(UnsignedLong.fromLongBits(i * 100))));
        }

        Collection<Point> pointsOut = AggregateFunctions.rate(pointsIn);

        assertEquals(pointsIn.size(), pointsOut.size());

        Iterator<Point> pointsIter = pointsOut.iterator();

        assertNull(pointsIter.next());

        while (pointsIter.hasNext()) {
            assertEquals(10, pointsIter.next().y.longValue());
        }

    }

    @Ignore
    @Test
    public void testTimestampIterator() {

        List<Timestamp> timestamps = Lists.newArrayList(getTimestamps(150, 3500));

        assertEquals(13, timestamps.size());
        assertEquals(new Timestamp(0, TimeUnit.SECONDS), timestamps.get(0));
        assertEquals(new Timestamp(3300, TimeUnit.SECONDS), timestamps.get(11));
        assertEquals(new Timestamp(3600, TimeUnit.SECONDS), timestamps.get(12));

        timestamps = Lists.newArrayList(getTimestamps(0, 3600));

        assertEquals(14, timestamps.size());
        assertEquals(new Timestamp(0, TimeUnit.SECONDS), timestamps.get(0));
        assertEquals(new Timestamp(3600, TimeUnit.SECONDS), timestamps.get(12));
        assertEquals(new Timestamp(3900, TimeUnit.SECONDS), timestamps.get(13));

    }

    private Iterable<Timestamp> getTimestamps(long startSecs, long endSecs) {
        return new AggregateFunctions.Timestamps(fromSeconds(startSecs), fromSeconds(endSecs));
    }

    private Timestamp fromSeconds(long value) {
        return new Timestamp(value, TimeUnit.SECONDS);
    }

}
