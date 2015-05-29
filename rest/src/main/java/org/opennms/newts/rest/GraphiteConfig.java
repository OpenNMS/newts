package org.opennms.newts.rest;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GraphiteConfig {

    @JsonProperty("enabled")
    private boolean m_enabled = false;

    @Min(value = 1024)
    @Max(value = 65535)
    @JsonProperty("port")
    private int m_port = 2003;

    public boolean isEnabled() {
        return m_enabled;
    }

    public int getPort() {
        return m_port;
    }

}
