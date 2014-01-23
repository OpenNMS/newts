package org.opennms.newts.api;


import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;


public class Aggregates {

    public static class Point {
        public Timestamp x;
        public ValueType<?> y;

        public Point(Timestamp x, ValueType<?> y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public String toString() {
            return String.format("%s[%s, %s]", getClass().getSimpleName(), x, y);
        }

    }

    static class Timestamps implements Iterable<Timestamp>, Iterator<Timestamp> {

        public static final long DEFAULT_STEP_SIZE = 300;
        public static final TimeUnit DEFAULT_STEP_UNITS = TimeUnit.SECONDS;

        private final long m_stepSize;
        private final TimeUnit m_stepUnits;

        private Timestamp m_current;
        private Timestamp m_final;

        public Timestamps(Timestamp start, Timestamp end) {
            this(start, end, DEFAULT_STEP_SIZE, DEFAULT_STEP_UNITS);
        }

        public Timestamps(Timestamp start, Timestamp finish, final long stepSize, final TimeUnit stepUnits) {
            m_current = start.stepCeiling(stepSize, stepUnits);
            m_final = finish.stepCeiling(stepSize, stepUnits);

            m_stepSize = stepSize;
            m_stepUnits = stepUnits;

        }

        @Override
        public boolean hasNext() {
            return m_current.lte(m_final);
        }

        @Override
        public Timestamp next() {

            if (!hasNext()) {
                throw new NoSuchElementException();
            }

            try {
                return m_current;
            }
            finally {
                m_current = m_current.add(m_stepSize, m_stepUnits);
            }
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Iterator<Timestamp> iterator() {
            return this;
        }

    }

    static class RateFunction implements Function<Point, Point> {

        private Point m_previous = null;

        @Override
        public Point apply(Point input) {

            if (m_previous == null) {
                m_previous = input;
                return null;
            }

            ValueType<?> rate = getRate(input);
            m_previous = input;

            return new Point(input.x, rate);
        }

        private ValueType<?> getRate(Point point) {
            return getCount(point).divideBy(getElapsedSeconds(point.x));
        }

        private ValueType<?> getCount(Point point) {
            return point.y.delta(m_previous.y);
        }

        private long getElapsedSeconds(Timestamp ts) {
            return ts.asSeconds() - m_previous.x.asSeconds();
        }

    }

    public static Collection<Point> rate(Collection<Point> points) {
        return Collections2.transform(points, new RateFunction());
    }

    public static final long HEARTBEAT = 300000;
    public static final double XFF = 0.5d;

    public static Collection<Point> rollup(Timestamp start, Timestamp end, long stepSize, TimeUnit stepUnits,
            Collection<Point> points) {

        List<Point> results = Lists.newArrayList();
        Iterator<Point> pointsIter = points.iterator();

        Point prevPoint = pointsIter.next();
        Point currPoint = pointsIter.next();
        ValueType<?> average = prevPoint.y, prevAverage = null;

        for (Timestamp step : new Timestamps(start, end, stepSize, stepUnits)) {

            int numIntervals = 0;
            int valid = 0, invalid = 0;

            while (true) {

                long elapsed = currPoint.x.asMillis() - prevPoint.x.asMillis();

                // If more than HEARTBEAT milliseconds have elapsed between this sample and the
                // last, disregard.
                if (elapsed > HEARTBEAT) {
                    invalid += elapsed;
                }
                else {
                    numIntervals += 1;
                    prevAverage = average;
                    average = average.times(numIntervals - 1).plus(currPoint.y).divideBy(numIntervals);
                    valid += elapsed;
                }

                // As soon as we encounter a sample that is equal-to or greater-than the our working
                // step, it's time to exit the loop.
                if (currPoint.x.gte(step) || !pointsIter.hasNext()) {
                    break;
                }
                else {
                    prevPoint = currPoint;
                    currPoint = pointsIter.next();
                }

            }

            // XFF (xfiles factor) determines how much of an interval can consist of unknown data.
            if ((invalid / (invalid + valid)) > XFF) {
                results.add(new Point(step, null));
                continue;
            }

            ValueType<?> value = interpolate(step, prevPoint.x, currPoint.x, prevAverage, average);
            results.add(new Point(step, value));

        }

        return results;
    }

    private static ValueType<?> interpolate(Timestamp x, Timestamp x0, Timestamp x1, ValueType<?> y0, ValueType<?> y1) {
        return interpolate(x.asMillis(), x0.asMillis(), x1.asMillis(), y0, y1);
    }

    private static ValueType<?> interpolate(long x, long x0, long x1, ValueType<?> y0, ValueType<?> y1) {
        return y1.minus(y0).times(x - x0).divideBy(x1 - x0).plus(y0);
    }

}
