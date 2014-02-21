package org.opennms.newts.rest;

import org.opennms.newts.api.MetricType;


public class SampleDTO {

    private long m_timestamp;
    private String m_resource;
    private String m_name;
    private MetricType m_type;
    private Number m_value;

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

    public String getName() {
        return m_name;
    }

    public void setName(String name) {
        m_name = name;
    }

    public MetricType getType() {
        return m_type;
    }

    public void setType(MetricType type) {
        m_type = type;
    }

    public Number getValue() {
        return m_value;
    }

    public void setValue(Number value) {
        m_value = value;
    }

}
