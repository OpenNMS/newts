package org.opennms.newts.rest;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;


public class MeasurementDTO {

    private final long m_timestamp;
    private final String m_resouce;
    private final String m_name;
    private final double m_value;

    @JsonCreator
    public MeasurementDTO(@JsonProperty("timestamp") long timestamp, @JsonProperty("resource") String resource, @JsonProperty("name") String name, @JsonProperty("value") double value) {
        m_timestamp = checkNotNull(timestamp, "timestamp argument");
        m_resouce = checkNotNull(resource, "resource argument");
        m_name = checkNotNull(name, "name argument");
        m_value = checkNotNull(value, "value argument");
    }

    public long getTimestamp() {
        return m_timestamp;
    }

    public String getResource() {
        return m_resouce;
    }

    public String getName() {
        return m_name;
    }

    public double getValue() {
        return m_value;
    }

}
