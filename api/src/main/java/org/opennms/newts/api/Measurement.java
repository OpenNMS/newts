package org.opennms.newts.api;


public class Measurement {

    private final Timestamp m_timestamp;
    private final String m_resource;
    private final String m_name;
    private final MetricType m_type;
    private final String m_units;
    private final double m_value;

    public Measurement(Timestamp timestamp, String resource, String name, MetricType type, double value) {
        this(timestamp, resource, name, type, null, value);
    }

    public Measurement(Timestamp timestamp, String resource, String name, MetricType type, String units, double value) {
        m_timestamp = timestamp;
        m_resource = resource;
        m_name = name;
        m_type = type;
        m_units = units;
        m_value = value;
    }

    public Timestamp getTimestamp() {
        return m_timestamp;
    }

    public String getResource() {
        return m_resource;
    }

    public String getName() {
        return m_name;
    }

    public MetricType getType() {
        return m_type;
    }

    public String getUnits() {
        return m_units;
    }

    public double getValue() {
        return m_value;
    }

}
