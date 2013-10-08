package org.opennms.newts.api;


public class Metric {

    private final String m_name;
    private final MetricType m_type;

    public Metric(String name, MetricType type) {
        m_name = name;
        m_type = type;
    }

    public String getName() {
        return m_name;
    }

    public MetricType getType() {
        return m_type;
    }

}
