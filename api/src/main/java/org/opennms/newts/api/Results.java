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
package org.opennms.newts.api;


import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import com.google.common.base.Objects;
import com.google.common.collect.Maps;


public class Results<T extends Element<?>> implements Iterable<Results.Row<T>>, Serializable {
    private static final long serialVersionUID = -3273508775312254315L;

    public static class Row<T extends Element<?>> implements Iterable<T>, Serializable {
        private static final long serialVersionUID = 4284597337435202235L;

        private Timestamp m_timestamp;
        private Resource m_resource;
        private Map<String, T> m_cells = Maps.newHashMap();

        public Row(Timestamp timestamp, Resource resource) {
            m_timestamp = timestamp;
            m_resource = resource;
        }

        public void addElement(T sample) {
            m_cells.put(sample.getName(), sample);
        }

        public T getElement(String name) {
            return m_cells.get(name);
        }

        public Timestamp getTimestamp() {
            return m_timestamp;
        }

        public Resource getResource() {
            return m_resource;
        }

        public Collection<T> getElements() {
            return m_cells.values();
        }

        @Override
        public Iterator<T> iterator() {
            return getElements().iterator();
        }

        @Override
        public String toString() {
            return String.format(
                    "%s[timestamp=%s, resource=%s, elements=%s",
                    getClass().getSimpleName(),
                    getTimestamp(),
                    getResource(),
                    getElements());
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            final Row<?> other = (Row<?>) obj;
            return Objects.equal(this.m_timestamp, other.m_timestamp)
                    && Objects.equal(this.m_resource, other.m_resource)
                    && Objects.equal(this.m_cells, other.m_cells);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(
                    this.m_timestamp, this.m_resource, this.m_cells);
        }
    }

    Map<Timestamp, Row<T>> m_rows = Maps.newTreeMap();

    public void addElement(T sample) {

        Row<T> row = m_rows.get(sample.getTimestamp());

        if (row == null) {
            row = new Row<T>(sample.getTimestamp(), sample.getResource());
            addRow(row);
        }

        row.addElement(sample);

    }

    public void addRow(Row<T> row) {
        m_rows.put(row.getTimestamp(), row);
    }

    public Collection<Row<T>> getRows() {
        return m_rows.values();
    }

    @Override
    public Iterator<Row<T>> iterator() {
        return getRows().iterator();
    }

    @Override
    public String toString() {
        return String.format("%s[%s]", getClass().getSimpleName(), getRows());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        final Results<?> other = (Results<?>) obj;
        return Objects.equal(this.m_rows, other.m_rows);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.m_rows);
    }
}
