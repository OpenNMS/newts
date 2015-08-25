package org.opennms.newts.cassandra;

import java.util.Collection;
import java.util.Map;

import org.opennms.newts.api.Context;
import org.opennms.newts.api.Duration;

import com.datastax.driver.core.ConsistencyLevel;
import com.google.common.collect.Maps;

public class ContextConfigurations {

    private static final Duration DEFAULT_RESOURCE_SHARD = Duration.days(7);

    private static final ConsistencyLevel DEFAULT_READ_CONSISTENCY = ConsistencyLevel.ANY;

    private static final ConsistencyLevel DEFAULT_WRITE_CONSISTENCY = ConsistencyLevel.ANY;

    private final Map<Context, ContextConfiguration> m_contexts = Maps.newHashMap();

    public ContextConfigurations() { }

    public ContextConfigurations(Collection<ContextConfiguration> contextConfigs) {
        for (ContextConfiguration contextConfig : contextConfigs) {
            addContextConfig(contextConfig);
        }
    }

    public ContextConfigurations addContextConfig(ContextConfiguration contextConfig) {
        m_contexts.put(contextConfig.getContext(), contextConfig);
        return this;
    }

    public ContextConfigurations addContextConfig(Context context, Duration resourceShard,
            ConsistencyLevel readConsistencyLevel, ConsistencyLevel writeConsistencyLevel) {
        m_contexts.put(context, new ContextConfiguration(context, resourceShard,
                readConsistencyLevel, writeConsistencyLevel));
        return this;
    }

    public Duration getResourceShard(Context context) {
        ContextConfiguration configConfig = m_contexts.get(context);
        if (configConfig != null) {
            return configConfig.getResourceShard();
        } else {
            return DEFAULT_RESOURCE_SHARD;
        }
    }

    public ConsistencyLevel getReadConsistency(Context context) {
        ContextConfiguration configConfig = m_contexts.get(context);
        if (configConfig != null) {
            return configConfig.getReadConsistency();
        } else {
            return DEFAULT_READ_CONSISTENCY;
        }
    }

    public ConsistencyLevel getWriteConsistency(Context context) {
        ContextConfiguration configConfig = m_contexts.get(context);
        if (configConfig != null) {
            return configConfig.getWriteConsistency();
        } else {
            return DEFAULT_WRITE_CONSISTENCY;
        }
    }
}
