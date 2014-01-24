package org.opennms.newts.api;


import java.util.Date;
import java.util.concurrent.TimeUnit;


public class Timestamp implements Comparable<Timestamp> {

    public final long m_time;
    public final TimeUnit m_unit;

    public Timestamp(long time, TimeUnit unit) {
        m_time = time;
        m_unit = unit;
    }

    private long convert(TimeUnit unit) {
        return unit.convert(m_time, m_unit);
    }

    public long asSeconds() {
        return convert(TimeUnit.SECONDS);
    }

    public long asMillis() {
        return convert(TimeUnit.MILLISECONDS);
    }

    public Date asDate() {
        return new Date(convert(TimeUnit.MILLISECONDS));
    }

    public TimeUnit getUnit() {
        return m_unit;
    }

    public Timestamp add(long value, TimeUnit units) {
        return new Timestamp(convert(units) + value, units);
    }

    public Timestamp subtract(long value, TimeUnit units) {
        return new Timestamp(convert(units) - value, units);
    }

    public boolean lt(Timestamp other) {
        return compareTo(other) < 0;
    }

    public boolean lte(Timestamp other) {
        return (lt(other) || equals(other));
    }

    public boolean gt(Timestamp other) {
        return compareTo(other) > 0;
    }

    public boolean gte(Timestamp other) {
        return (gt(other) || equals(other));
    }

    public Timestamp stepFloor(long stepSize, TimeUnit units) {
        return new Timestamp((convert(units) / stepSize) * stepSize, units);
    }

    public Timestamp stepCeiling(long stepSize, TimeUnit units) {
        return new Timestamp(((convert(units) / stepSize) + 1) * stepSize, units);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Timestamp)) return false;
        return compareTo((Timestamp) other) == 0;
    }

    @Override
    public int compareTo(Timestamp o) {
        return asMillis() < o.asMillis() ? -1 : asMillis() > o.asMillis() ? 1 : 0;
    }

    @Override
    public String toString() {
        return String.format("%s[%s]", getClass().getSimpleName(), asDate());
    }

    public static Timestamp now() {
        return new Timestamp(System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }

}
