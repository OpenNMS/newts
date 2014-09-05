package org.opennms.newts.cassandra.search;


import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;


public class ResourceMetadata {

    private final Set<String> m_metrics = Sets.newHashSet();
    private final Map<String, String> m_attributes = Maps.newHashMap();

    public boolean containsMetric(String metric) {
        return m_metrics.contains(metric);
    }

    public void putMetric(String metric) {
        m_metrics.add(metric);
    }

    public boolean containsAttribute(String key, String value) {
        return m_attributes.containsKey(key) && m_attributes.get(key).equals(value);
    }

    public void putAttribute(String key, String value) {
        m_attributes.put(key, value);
    }

}
