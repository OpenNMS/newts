package org.opennms.newts.rest;


public class Measurement {

    private long m_timestamp;
    private String m_resource;
    private Metric m_metric;
    private double m_value;

    public long getTimestamp() {
        return m_timestamp;
    }

    public void setTimestamp(long timestamp) {
        m_timestamp = timestamp;
    }

    public String getResource() {
        return m_resource;
    }

    public void setResource(String resource) {
        m_resource = resource;
    }

    public Metric getMetric() {
        return m_metric;
    }

    public void setMetric(Metric metric) {
        m_metric = metric;
    }

    public double getValue() {
        return m_value;
    }

    public void setValue(double value) {
        m_value = value;
    }

}
