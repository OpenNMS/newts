package org.opennms.newts.cassandra.search;


import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;


public class ResourceMetadata {

    private final Set<String> m_metrics = Sets.newConcurrentHashSet();
    private final Map<String, String> m_attributes = Maps.newConcurrentMap();

    public boolean containsMetric(String metric) {
        return m_metrics.contains(metric);
    }

    public ResourceMetadata putMetric(String metric) {
        m_metrics.add(metric);
        return this;
    }

    public boolean containsAttribute(String key, String value) {
        return m_attributes.containsKey(key) && m_attributes.get(key).equals(value);
    }

    public ResourceMetadata putAttribute(String key, String value) {
        m_attributes.put(key, value);
        return this;
    }

    public void merge(ResourceMetadata other) {
        m_metrics.addAll(other.m_metrics);
        m_attributes.putAll(other.m_attributes);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((m_attributes == null) ? 0 : m_attributes.hashCode());
        result = prime * result + ((m_metrics == null) ? 0 : m_metrics.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        ResourceMetadata other = (ResourceMetadata) obj;
        if (m_attributes == null) {
            if (other.m_attributes != null) return false;
        }
        else if (!m_attributes.equals(other.m_attributes)) return false;
        if (m_metrics == null) {
            if (other.m_metrics != null) return false;
        }
        else if (!m_metrics.equals(other.m_metrics)) return false;
        return true;
    }

}
