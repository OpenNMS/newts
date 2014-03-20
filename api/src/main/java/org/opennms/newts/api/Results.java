package org.opennms.newts.api;


import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.Maps;


@JsonSerialize(using = ResultsSerializer.class)
public class Results<T extends Element<?>> implements Iterable<Results.Row<T>> {

    public static class Row<T extends Element<?>> implements Iterable<T> {

        private Timestamp m_timestamp;
        private String m_resource;
        private Map<String, T> m_cells = Maps.newHashMap();

        public Row(Timestamp timestamp, String resource) {
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

        public String getResource() {
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
