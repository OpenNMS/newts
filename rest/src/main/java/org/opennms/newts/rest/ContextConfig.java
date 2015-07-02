package org.opennms.newts.rest;

import org.opennms.newts.api.Context;
import org.opennms.newts.api.Duration;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ContextConfig {

    @JsonProperty("id")
    private String m_id;

    @JsonProperty("resource-shard")
    private String m_resourceShard;

    public Context getContext() {
        return new Context(m_id);
    }

    public Duration getResourceShard() {
        return Duration.parse(m_resourceShard);
    }

}
