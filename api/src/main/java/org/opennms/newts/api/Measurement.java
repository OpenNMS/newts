package org.opennms.newts.api;


public class Measurement {

    private final Timestamp m_timestamp;
    private final String m_resource;
    private final Metric m_metric;
    private final double m_value;

    public Measurement(Timestamp timestamp, String resource, Metric metric, double value) {
        m_timestamp = timestamp;
        m_resource = resource;
        m_metric = metric;
        m_value = value;
    }

    public Timestamp getTimestamp() {
        return m_timestamp;
    }

    public String getResource() {
        return m_resource;
    }

    public Metric getMetric() {
        return m_metric;
    }

    public double getValue() {
        return m_value;
    }

}
