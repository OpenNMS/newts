package org.opennms.newts.rest;


import org.opennms.newts.api.MetricType;


public class Metric {

    private String m_name;
    private MetricType m_type;

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

}
