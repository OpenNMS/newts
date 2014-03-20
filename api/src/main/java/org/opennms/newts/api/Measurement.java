package org.opennms.newts.api;


import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;


@JsonSerialize(using=MeasurementSerializer.class)
public class Measurement implements Element<Double> {

    private final Timestamp m_timestamp;
    private final String m_resource;
    private final String m_name;
    private final double m_value;
    private final Map<String, String> m_attributes;

    public Measurement(Timestamp timestamp, String resource, String name, double value) {
        this(timestamp, resource, name, value, null);
    }

    public Measurement(Timestamp timestamp, String resource, String name, double value, Map<String, String> attributes) {
        m_timestamp = checkNotNull(timestamp, "timestamp");
        m_resource = checkNotNull(resource, "resource");
        m_name = checkNotNull(name, "name");
        m_value = checkNotNull(value, "value");
        m_attributes = attributes;
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

    public Double getValue() {
        return m_value;
    }

    public Map<String, String> getAttributes() {
        return m_attributes;
    }

    @Override
    public String toString() {
        return String.format(
                "%s[timestamp=%s, resource=%s, name=%s, value=%s]",
                getClass().getSimpleName(),
                getTimestamp(),
                getResource(),
                getName(),
                getValue());
    }

}
