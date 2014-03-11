package org.opennms.newts.rest;


import static com.google.common.base.Preconditions.checkNotNull;

import org.opennms.newts.api.MetricType;
import org.opennms.newts.api.Sample;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * Read-only data transfer object for {@link Sample}s.
 * 
 * @author eevans
 */
public class SampleDTO {

    private final long m_timestamp;
    private final String m_resource;
    private final String m_name;
    private final MetricType m_type;
    private final Number m_value;

    @JsonCreator
    public SampleDTO(@JsonProperty("timestamp") long timestamp, @JsonProperty("resource") String resource, @JsonProperty("name") String name, @JsonProperty("type") MetricType type, @JsonProperty("value") Number value) {
        m_timestamp = checkNotNull(timestamp, "m_timestamp argument");
        m_resource = checkNotNull(resource, "m_resource argument");
        m_name = checkNotNull(name, "m_name argument");
        m_type = checkNotNull(type, "m_type argument");
        m_value = checkNotNull(value, "m_value argument");
    }

    public long getTimestamp() {
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

    public Number getValue() {
        return m_value;
    }

}
