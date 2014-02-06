package org.opennms.newts.api;


import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;


public class AggregateFunctions {

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

        public static final Duration DEFAULT_STEP_SIZE = Duration.seconds(300);

        private final Duration m_stepSize;

        private Timestamp m_current;
        private Timestamp m_final;

        public Timestamps(Timestamp start, Timestamp end) {
            this(start, end, DEFAULT_STEP_SIZE);
        }

        public Timestamps(Timestamp start, Timestamp finish, final Duration stepSize) {
            m_current = start.stepFloor(stepSize);
            m_final = finish.stepCeiling(stepSize);
            m_stepSize = stepSize;
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
                m_current = m_current.plus(m_stepSize);
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

    static class Averager implements Iterable<Point>, Iterator<Point> {

        private static final double XFF = 0.5d;

        private final Timestamp m_start;
        private final Duration m_heartbeat;
        private final Iterator<Point> m_points;
        private final Iterator<Timestamp> m_steps;

        private Timestamp m_nextStep;
        private Timestamp m_lastUpdate;
        private ValueType<?> m_accumulated = new Gauge(0.0d);
        private long m_unknown;
        private long m_known;

        public Averager(Timestamp start, Timestamp end, Duration stepSize, Iterator<Point> points) {
            this(start, end, stepSize, stepSize.times(2), points);
        }

        public Averager(Timestamp start, Timestamp end, Duration stepSize, Duration heartbeat, Iterator<Point> points) {

            checkNotNull(start, "start argument");
            checkNotNull(end, "end argument");
            checkNotNull(stepSize, "stepSize argument");
            checkNotNull(heartbeat, "heartbeat argument");
            checkNotNull(points, "points argument");

            m_start = start;
            m_heartbeat = heartbeat;
            m_points = points;
            m_steps = new Timestamps(start, end, stepSize);

            m_nextStep = m_steps.next();
            m_lastUpdate = start;
            m_unknown = m_lastUpdate.asMillis() % stepSize.asMillis();
            m_known = 0;

        }

        @Override
        public boolean hasNext() {
            return m_nextStep != null;
        }

        @Override
        public Point next() {
            if (m_nextStep.lt(m_start)) {
                try {
                    return new Point(m_nextStep, null);
                }
                finally {
                    m_nextStep = m_steps.next();
                }
            }

            Point point = null, result = new Point(m_nextStep, null);
            for (; m_points.hasNext();) {
                point = m_points.next();

                // it would probably be better to use 'before the start data if you have it to
                // provide a value for the
                // initial unknown region
                if (point == null || point.x.lt(m_lastUpdate)) {
                    continue;
                }

                accumulate(point);

                if (point.x.gte(m_nextStep)) {
                    result.y = getAccummulatedAverage();

                    // Accumulate the remainder
                    long interval = point.x.asMillis() - m_nextStep.asMillis();
                    m_known = interval;
                    m_accumulated = new Gauge(0.0d).plus(point.y.times(interval));

                    m_nextStep = m_steps.hasNext() ? m_steps.next() : null;

                    m_unknown = 0;
                    m_lastUpdate = point.x;
                    break;
                }

                m_lastUpdate = point.x;

            }

            return result;
        }

        /** Returns the accumulated average for the "step" under consideration. */
        private ValueType<?> getAccummulatedAverage() {
            long elapsed = m_known + m_unknown;
            return (elapsed > 0 && (m_unknown / elapsed) < XFF) ? m_accumulated.divideBy(m_known) : null;
        }

        /** Accumulates this point. */
        private void accumulate(Point point) {
            long interval = getInterval(point);

            if (point.y != null && interval < m_heartbeat.asMillis()) {
                m_known += interval;
                m_accumulated = m_accumulated.plus(point.y.times(interval));
            }
            else {
                m_unknown += interval;
            }
        }

        private long getInterval(Point point) {
            long interval;

            if (point.x.gte(m_nextStep)) {
                interval = m_nextStep.asMillis() - m_lastUpdate.asMillis();
            }
            else {
                interval = point.x.asMillis() - m_lastUpdate.asMillis();
            }

            return interval;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Iterator<Point> iterator() {
            return this;
        }

    }

    public static Iterable<Point> average(Timestamp start, Timestamp end, Duration stepSize, Collection<Point> points) {
        return new Averager(start, end, stepSize, points.iterator());
    }

}
