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
package org.opennms.newts.api;


import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;
import java.util.Map;

import com.google.common.base.Objects;


public class Sample implements Element<ValueType<?>>, Serializable {
    private static final long serialVersionUID = 3819211879010998577L;

    private final Timestamp m_timestamp;
    private final Context m_context;
    private final Resource m_resource;
    private final String m_name;
    private final MetricType m_type;
    private final ValueType<?> m_value;
    private final Map<String, String> m_attributes;

    public Sample(Timestamp timestamp, Resource resource, String name, MetricType type, ValueType<?> value) {
        this(timestamp, Context.DEFAULT_CONTEXT, resource, name, type, value, null);
    }

    public Sample(Timestamp timestamp, Resource resource, String name, MetricType type, ValueType<?> value, Map<String, String> attributes) {
        this(timestamp, Context.DEFAULT_CONTEXT, resource, name, type, value, attributes);
    }

    public Sample(Timestamp timestamp, Context context, Resource resource, String name, MetricType type, ValueType<?> value) {
        this(timestamp, context, resource, name, type, value, null);
    }
    
    public Sample(Timestamp timestamp, Context context, Resource resource, String name, MetricType type, ValueType<?> value, Map<String, String> attributes) {
        m_timestamp = checkNotNull(timestamp, "timestamp");
        m_context = checkNotNull(context, "context argument");
        m_resource = checkNotNull(resource, "resource");
        m_name = checkNotNull(name, "name");
        m_type = checkNotNull(type, "type");
        m_value = value;
        m_attributes = attributes;
    }

    public Timestamp getTimestamp() {
        return m_timestamp;
    }

    public Context getContext() {
        return m_context;
    }

    public Resource getResource() {
        return m_resource;
    }

    public String getName() {
        return m_name;
    }

    public MetricType getType() {
        return m_type;
    }

    public ValueType<?> getValue() {
        return m_value;
    }

    public Map<String, String> getAttributes() {
        return m_attributes;
    }

    @Override
    public String toString() {
        return String.format(
                "%s[timestamp=%s, context=%s, resource=%s, name=%s, type=%s, value=%s]",
                getClass().getSimpleName(),
                getTimestamp(),
                getContext(),
                getResource(),
                getName(),
                getType(),
                getValue());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        final Sample other = (Sample) obj;
        return Objects.equal(this.m_timestamp, other.m_timestamp)
                && Objects.equal(this.m_context, other.m_context)
                && Objects.equal(this.m_resource, other.m_resource)
                && Objects.equal(this.m_name, other.m_name)
                && Objects.equal(this.m_type, other.m_type)
                && Objects.equal(this.m_value, other.m_value)
                && Objects.equal(this.m_attributes, other.m_attributes);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(
                this.m_timestamp, this.m_context, this.m_resource, this.m_name, m_type, this.m_value, this.m_attributes);
    }
}
