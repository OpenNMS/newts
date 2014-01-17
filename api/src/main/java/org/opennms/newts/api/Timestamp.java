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

    public Timestamp add(Timestamp other) {
        return new Timestamp(convert(TimeUnit.MILLISECONDS) + other.asMillis(), TimeUnit.MILLISECONDS);
    }

    public boolean lessThan(Timestamp other) {
        return compareTo(other) < 0;
    }

    public boolean greaterThan(Timestamp other) {
        return compareTo(other) > 0;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Timestamp)) return false;
        return compareTo((Timestamp)other) == 0;
    }
    
    @Override
    public int compareTo(Timestamp o) {
        return asMillis() < o.asMillis() ? -1 : asMillis() > o.asMillis() ? 1 : 0;
    }

    public static Timestamp now() {
        return new Timestamp(System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }

}
