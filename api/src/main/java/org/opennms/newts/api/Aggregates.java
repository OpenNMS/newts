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
            m_current = start.stepFloor(stepSize, stepUnits);
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

    public static final long HEARTBEAT = 600000;
    public static final double XFF = 0.5d;

    public static Collection<Point> average(Timestamp start, Timestamp end, long stepSize, TimeUnit stepUnits,
            Collection<Point> points) {

        List<Point> results = Lists.newArrayList();
        Iterator<Timestamp> steps = new Timestamps(start, end, stepSize, stepUnits);

        Timestamp nextStep = steps.next();
        Timestamp lastUpdate = start;
        ValueType<?> accumulated = new Gauge(0.0d);
        long unknown = lastUpdate.asMillis() % TimeUnit.MILLISECONDS.convert(stepSize, stepUnits), known = 0;

        for (Point point : points) {
            if (point.x.lt(lastUpdate)) {
                continue;
            }

            long interval;

            if (point.x.gte(nextStep)) {
                interval = nextStep.asMillis() - lastUpdate.asMillis();
            }
            else {
                interval = point.x.asMillis() - lastUpdate.asMillis();
            }

            if (point.y != null && interval < HEARTBEAT) {
                known += interval;
                accumulated = accumulated.plus(point.y.times(interval));
            }
            else {
                unknown += interval;
            }

            if (point.x.gte(nextStep)) {
                ValueType<?> value = null;
                long elapsed = known + unknown;
                if (elapsed > 0 && (unknown / elapsed) < XFF) {
                    value = accumulated.divideBy(known);
                }
                results.add(new Point(nextStep, value));

                // Accumulate the remainder
                interval = point.x.asMillis() - nextStep.asMillis();
                known = interval;
                accumulated = new Gauge(0.0d).plus(point.y.times(interval));

                if (!steps.hasNext()) {
                    break;
                }

                nextStep = steps.next();
                unknown = 0;
            }

            lastUpdate = point.x;

        }

        return results;
    }

}
