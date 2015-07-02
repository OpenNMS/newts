package org.opennms.newts.persistence.cassandra;

import java.util.Collection;
import java.util.Map;

import org.opennms.newts.api.Context;
import org.opennms.newts.api.Duration;

import com.google.common.collect.Maps;

public class ContextConfigurations {

    // Default resource shard
    private static final Duration DEFAULT_RESOURCE_SHARD = Duration.days(7);

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

    public ContextConfigurations addContextConfig(Context context, Duration resourceShard) {
        return addContextConfig(new ContextConfiguration(context, resourceShard));
    }

    public Duration getResourceShard(Context context) {
        ContextConfiguration configConfig = m_contexts.get(context);
        if (configConfig != null) {
            return configConfig.getResourceShard();
        } else {
            return DEFAULT_RESOURCE_SHARD;
        }
    }
}
