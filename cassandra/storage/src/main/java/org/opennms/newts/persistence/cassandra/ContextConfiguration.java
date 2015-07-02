package org.opennms.newts.persistence.cassandra;

import org.opennms.newts.api.Context;
import org.opennms.newts.api.Duration;

import static com.google.common.base.Preconditions.checkNotNull;

public class ContextConfiguration {

    private final Context m_context;
    private final Duration m_resourceShard;

    public ContextConfiguration(Context context, Duration resourceShard) {
        m_context = checkNotNull(context, "context argument");
        m_resourceShard = checkNotNull(resourceShard, "resourceShard argument");;
    }

    public Context getContext() { 
        return m_context;
    }

    public Duration getResourceShard() {
        return m_resourceShard;
    }
}
