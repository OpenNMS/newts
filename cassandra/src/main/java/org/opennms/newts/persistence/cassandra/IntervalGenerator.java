package org.opennms.newts.persistence.cassandra;


import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.opennms.newts.api.Duration;
import org.opennms.newts.api.Timestamp;


/**
 * Aligned, fixed-interval iterator of {@link Timestamp}s.
 * 
 * @author eevans
 */
public class IntervalGenerator implements Iterator<Timestamp>, Iterable<Timestamp> {

    private final Duration m_interval;
    private final Timestamp m_final;
    private Timestamp m_current;

    public IntervalGenerator(Timestamp start, Timestamp finish, Duration interval) {
        m_interval = checkNotNull(interval, "interval argument");
        m_current = checkNotNull(start, "start argument").stepFloor(m_interval);
        m_final = checkNotNull(finish, "finish argument").stepCeiling(m_interval);
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
            m_current = m_current.plus(m_interval);
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
