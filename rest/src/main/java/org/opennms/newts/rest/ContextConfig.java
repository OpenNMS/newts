package org.opennms.newts.rest;

import org.opennms.newts.api.Context;
import org.opennms.newts.api.Duration;

import com.datastax.driver.core.ConsistencyLevel;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ContextConfig {

    @JsonProperty("id")
    private String m_id;

    @JsonProperty("resource-shard")
    private String m_resourceShard;

    @JsonProperty("read-consistency")
    private String m_readConsistency;

    @JsonProperty("write-consistency")
    private String m_writeConsistency;

    public Context getContext() {
        return new Context(m_id);
    }

    public Duration getResourceShard() {
        return Duration.parse(m_resourceShard);
    }

    public ConsistencyLevel getReadConsistency() {
        return ConsistencyLevel.valueOf(m_readConsistency);
    }

    public ConsistencyLevel getWriteConsistency() {
        return ConsistencyLevel.valueOf(m_writeConsistency);
    }
}
