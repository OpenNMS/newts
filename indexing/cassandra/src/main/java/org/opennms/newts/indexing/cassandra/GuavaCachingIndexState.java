package org.opennms.newts.indexing.cassandra;


import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map.Entry;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Multimap;


@Singleton
public class GuavaCachingIndexState implements IndexState {

    private static final Object PRESENT = new Object();

    private final Cache<String, Object> m_state;
    private final Counter m_hitCounter;
    private final Counter m_missCounter;
    private final Counter m_requestCounter;

    @Inject
    public GuavaCachingIndexState(@Named("indexState.maxSize") int maxSize, MetricRegistry metricRegistry) {
        m_state = CacheBuilder.newBuilder().maximumSize(maxSize).build();
        checkNotNull(metricRegistry, "metric registry argument");

        m_hitCounter = metricRegistry.counter(MetricRegistry.name(GuavaCachingIndexState.class, "hitCount"));
        m_missCounter = metricRegistry.counter(MetricRegistry.name(GuavaCachingIndexState.class, "missCount"));
        m_requestCounter = metricRegistry.counter(MetricRegistry.name(GuavaCachingIndexState.class, "requests"));

    }

    @Override
    public void put(String resource, String metric) {
        m_state.put(keyFor(resource, metric), PRESENT);
    }

    @Override
    public void putAll(Multimap<String, String> metrics) {
        for (Entry<String, String> entry : metrics.entries()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public boolean exists(String resource, String metric) {
        m_requestCounter.inc();

        Object obj = m_state.getIfPresent(keyFor(resource, metric));
        if (obj != null) m_hitCounter.inc(); else m_missCounter.inc();

        return obj != null;
    }

    private String keyFor(String resource, String metric) {
        return resource + metric;
    }

}
