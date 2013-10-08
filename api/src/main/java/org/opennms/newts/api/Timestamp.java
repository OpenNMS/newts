package org.opennms.newts.api;


import java.util.Date;
import java.util.concurrent.TimeUnit;


public class Timestamp implements Comparable<Timestamp> {

    public final long m_time;
    public final TimeUnit m_unit;

    public Timestamp(long time) {
        this(time, TimeUnit.MILLISECONDS);
    }

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

    @Override
    public int compareTo(Timestamp o) {
        return asMillis() < o.asMillis() ? -1 : asMillis() > o.asMillis() ? 1 : 0;
    }

}
