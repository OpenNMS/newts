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


import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import com.google.common.collect.Maps;


public class Results<T extends Element<?>> implements Iterable<Results.Row<T>> {

    public static class Row<T extends Element<?>> implements Iterable<T> {

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

}
