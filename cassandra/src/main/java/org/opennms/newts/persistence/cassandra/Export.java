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
package org.opennms.newts.persistence.cassandra;


import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

import org.opennms.newts.api.Measurement;
import org.opennms.newts.api.Results.Row;


/**
 * Filter results to a specified set of exports.
 *
 * @author eevans
 *
 */
class Export implements Iterable<Row<Measurement>>, Iterator<Row<Measurement>> {

    private final Set<String> m_exports;
    private final Iterator<Row<Measurement>> m_input;

    private Row<Measurement> m_current;

    Export(Set<String> exports, Iterator<Row<Measurement>> input) {
        m_exports = checkNotNull(exports, "exports argument");
        m_input = checkNotNull(input, "input argument");

        m_current = m_input.hasNext() ? m_input.next() : null;

    }

    @Override
    public boolean hasNext() {
        return m_current != null;
    }

    @Override
    public Row<Measurement> next() {

        if (!hasNext()) throw new NoSuchElementException();

        Row<Measurement> result = new Row<>(m_current.getTimestamp(), m_current.getResource());

        for (String export : m_exports) {
            result.addElement(getMeasurement(export));
        }

        try {
            return result;
        }
        finally {
            m_current = m_input.hasNext() ? m_input.next() : null;
        }
    }

    private Measurement getMeasurement(String name) {
        Measurement measurement = m_current.getElement(name);
        return (measurement != null) ? measurement : getNan(name);
    }

    private Measurement getNan(String name) {
        return new Measurement(m_current.getTimestamp(), m_current.getResource(), name, Double.NaN);
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<Row<Measurement>> iterator() {
        return this;
    }

}
