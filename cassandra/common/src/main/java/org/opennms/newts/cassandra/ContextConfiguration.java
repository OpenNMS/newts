package org.opennms.newts.cassandra;

import org.opennms.newts.api.Context;
import org.opennms.newts.api.Duration;

import com.datastax.oss.driver.api.core.ConsistencyLevel;

import static com.google.common.base.Preconditions.checkNotNull;

public class ContextConfiguration {

    private final Context m_context;
    private final Duration m_resourceShard;
    private final ConsistencyLevel m_readConsistency;
    private final ConsistencyLevel m_writeConsistency;

    public ContextConfiguration(Context context, Duration resourceShard,
            ConsistencyLevel readConsistency, ConsistencyLevel writeConsistency) {
        m_context = checkNotNull(context, "context argument");
        m_resourceShard = checkNotNull(resourceShard, "resourceShard argument");
        m_readConsistency = checkNotNull(readConsistency, "readConsistency argument");
        m_writeConsistency = checkNotNull(writeConsistency, "writeConsistency argument");
    }

    public Context getContext() { 
        return m_context;
    }

    public Duration getResourceShard() {
        return m_resourceShard;
    }

    public ConsistencyLevel getReadConsistency() {
        return m_readConsistency;
    }

    public ConsistencyLevel getWriteConsistency() {
        return m_writeConsistency;
    }
}
