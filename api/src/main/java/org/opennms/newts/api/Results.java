package org.opennms.newts.api;


import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import com.google.common.collect.Maps;


public class Results implements Iterable<Results.Row> {

    public static class Row implements Iterable<Measurement> {

        private Timestamp m_timestamp;
        private String m_resource;
        private Map<Metric, Measurement> m_cells = Maps.newHashMap();

        public Row(Timestamp timestamp, String resource) {
            m_timestamp = timestamp;
            m_resource = resource;
        }

        public void addMeasurement(Measurement measurement) {
            m_cells.put(measurement.getMetric(), measurement);
        }

        public Measurement getMeasurement(Metric metric) {
            return m_cells.get(metric);
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

    }

    Map<Timestamp, Row> m_rows = Maps.newTreeMap();

    public void addMeasurement(Measurement measurement) {

        Row row = m_rows.get(measurement.getTimestamp());

        if (row == null) {
            row = new Row(measurement.getTimestamp(), measurement.getResource());
            m_rows.put(measurement.getTimestamp(), row);
        }

        row.addMeasurement(measurement);

    }

    public Collection<Row> getRows() {
        return m_rows.values();
    }

    @Override
    public Iterator<Row> iterator() {
        return getRows().iterator();
    }

}
