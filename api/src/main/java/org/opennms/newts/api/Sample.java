package org.opennms.newts.api;


import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize(using=SampleSerializer.class)
public class Sample implements Element<ValueType<?>>{

    private final Timestamp m_timestamp;
    private final String m_resource;
    private final String m_name;
    private final MetricType m_type;
    private final ValueType<?> m_value;
    private final Map<String, String> m_attributes;

    public Sample(Timestamp timestamp, String resource, String name, MetricType type, ValueType<?> value) {
        this(timestamp, resource, name, type, value, null);
    }

    public Sample(Timestamp timestamp, String resource, String name, MetricType type, ValueType<?> value, Map<String, String> attributes) {
        m_timestamp = checkNotNull(timestamp, "timestamp");
        m_resource = checkNotNull(resource, "resource");
        m_name = checkNotNull(name, "name");
        m_type = checkNotNull(type, "type");
        m_value = value;
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

    public MetricType getType() {
        return m_type;
    }

    public ValueType<?> getValue() {
        return m_value;
    }

    public Map<String, String> getAttributes() {
        return m_attributes;
    }

    @Override
    public String toString() {
        return String.format(
                "%s[timestamp=%s, resource=%s, name=%s, type=%s, value=%s]",
                getClass().getSimpleName(),
                getTimestamp(),
                getResource(),
                getName(),
                getType(),
                getValue());
    }

}
