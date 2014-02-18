package org.opennms.newts.api;


import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import com.google.common.collect.Maps;


public class Results implements Iterable<Results.Row> {

    public static class Row implements Iterable<Measurement> {

        private Timestamp m_timestamp;
        private String m_resource;
        private Map<String, Measurement> m_cells = Maps.newHashMap();

        public Row(Timestamp timestamp, String resource) {
            m_timestamp = timestamp;
            m_resource = resource;
        }

        public void addMeasurement(Measurement measurement) {
            m_cells.put(measurement.getName(), measurement);
        }

        public Measurement getMeasurement(String name) {
            return m_cells.get(name);
        }

        public Timestamp getTimestamp() {
            return m_timestamp;
        }

        public String getResource() {
            return m_resource;
        }

        public Collection<Measurement> getMeasurements() {
            return m_cells.values();
        }

        @Override
        public Iterator<Measurement> iterator() {
            return getMeasurements().iterator();
        }

        @Override
        public String toString() {
            return String.format("%s[timestamp=%s, resource=%s", getClass().getSimpleName(), getTimestamp(), getResource());
        }

    }

    Map<Timestamp, Row> m_rows = Maps.newTreeMap();

    public void addMeasurement(Measurement measurement) {

        Row row = m_rows.get(measurement.getTimestamp());

        if (row == null) {
            row = new Row(measurement.getTimestamp(), measurement.getResource());
            addRow(row);
        }

        row.addMeasurement(measurement);

    }

    public void addRow(Row row) {
        m_rows.put(row.getTimestamp(), row);
    }

    public Collection<Row> getRows() {
        return m_rows.values();
    }

    @Override
    public Iterator<Row> iterator() {
        return getRows().iterator();
    }

}
