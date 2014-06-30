/*
 * Copyright 2014, The OpenNMS Group
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.opennms.newts.aggregate;


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
    private final boolean m_reversed;
    private Timestamp m_current;

    public IntervalGenerator(Timestamp start, Timestamp finish, Duration interval) {
        this(start, finish, interval, false);
    }
    
    public IntervalGenerator(Timestamp start, Timestamp finish, Duration interval, boolean reversed) {
        m_interval = checkNotNull(interval, "interval argument");
        checkNotNull(start, "start argument");
        checkNotNull(finish, "finish argument");

        m_current = reversed ? finish : start;
        m_final = reversed ? start : finish;
        m_reversed = reversed;

    }

    @Override
    public boolean hasNext() {
        return  m_reversed ? m_current.gte(m_final) : m_current.lte(m_final);
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
            m_current = m_reversed ? m_current.minus(m_interval) : m_current.plus(m_interval);
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
