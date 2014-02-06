package org.opennms.newts.api;


import java.util.concurrent.TimeUnit;


public class Duration {

    private final long m_duration;
    private final TimeUnit m_unit;

    public Duration(long duration, TimeUnit unit) {
        m_duration = duration;
        m_unit = unit;
    }

    public long convert(TimeUnit unit) {
        return unit.convert(getDuration(), getUnit());
    }

    public long asMillis() {
        return convert(TimeUnit.MILLISECONDS);
    }

    public long asSeconds() {
        return convert(TimeUnit.SECONDS);
    }

    public static Duration seconds(long seconds) {
        return new Duration(seconds, TimeUnit.SECONDS);
    }

    public static Duration millis(long millis) {
        return new Duration(millis, TimeUnit.MILLISECONDS);
    }

    public long getDuration() {
        return m_duration;
    }

    public TimeUnit getUnit() {
        return m_unit;
    }

    public Duration times(long value) {
        return new Duration(getDuration() * value, getUnit());
    }

    public String toString() {
        return String.format("%s[%d, %s]", getClass().getSimpleName(), m_duration, getUnit());
    }

}

