/*
 * Copyright 2014, The OpenNMS Group
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.opennms.newts.cassandra.search;


import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

import com.codahale.metrics.Meter;
import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;


public class ResourceMetadata implements Serializable {

    private static final long serialVersionUID = 2103401685689076369L;

    private final Set<String> m_metrics = Sets.newConcurrentHashSet();
    private final Map<String, String> m_attributes = Maps.newConcurrentMap();
    private final transient Optional<Meter> m_metricReqs;
    private final transient Optional<Meter> m_attributeReqs;
    private final transient Optional<Meter> m_metricMisses;
    private final transient Optional<Meter> m_attributeMisses;

    public ResourceMetadata(Meter metricReqs, Meter attributeReqs, Meter metricMisses, Meter attributeMisses) {
        m_metricReqs = Optional.of(checkNotNull(metricReqs, "metricsReqs argument"));
        m_attributeReqs = Optional.of(checkNotNull(attributeReqs, "attributesReqs argument"));
        m_metricMisses = Optional.of(checkNotNull(metricMisses, "metricsMisses argument"));
        m_attributeMisses = Optional.of(checkNotNull(attributeMisses, "attributesMisses argument"));
    }

    public ResourceMetadata() {
        m_metricReqs = Optional.absent();
        m_attributeReqs = Optional.absent();
        m_metricMisses = Optional.absent();
        m_attributeMisses = Optional.absent();
    }

    public boolean containsMetric(String metric) {
        if (m_metricReqs.isPresent()) m_metricReqs.get().mark();
        boolean contains = m_metrics.contains(metric);
        if ((!contains) && m_metricMisses.isPresent()) m_metricMisses.get().mark(); 
        return contains;
    }

    public ResourceMetadata putMetric(String metric) {
        m_metrics.add(metric);
        return this;
    }

    public boolean containsAttribute(String key, String value) {
        if (m_attributeReqs.isPresent()) m_attributeReqs.get().mark();
        boolean contains = m_attributes.containsKey(key) && m_attributes.get(key).equals(value);
        if ((!contains) && m_attributeMisses.isPresent()) m_attributeMisses.get().mark();
        return contains;
    }

    public ResourceMetadata putAttribute(String key, String value) {
        m_attributes.put(key, value);
        return this;
    }

    /**
     * Merges the metrics and attributes from the given instance, to the current instance.
     *
     * @param other a {@link ResourceMetadata} object
     * @return true if the meta-data was modified as a result of the merge, false otherwise
     */
    public boolean merge(ResourceMetadata other) {
        boolean modified = m_metrics.addAll(other.m_metrics);
        if (!modified) {
            modified = !m_attributes.equals(other.m_attributes);
        }
        m_attributes.putAll(other.m_attributes);
        return modified;
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
