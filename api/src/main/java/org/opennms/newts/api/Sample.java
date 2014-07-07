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

import java.util.Map;


public class Sample implements Element<ValueType<?>>{

    private final Timestamp m_timestamp;
    private final Resource m_resource;
    private final String m_name;
    private final MetricType m_type;
    private final ValueType<?> m_value;
    private final Map<String, String> m_attributes;

    public Sample(Timestamp timestamp, Resource resource, String name, MetricType type, ValueType<?> value) {
        this(timestamp, resource, name, type, value, null);
    }

    public Sample(Timestamp timestamp, Resource resource, String name, MetricType type, ValueType<?> value, Map<String, String> attributes) {
        m_timestamp = checkNotNull(timestamp, "timestamp");
        m_resource = checkNotNull(resource, "resource");
        m_name = checkNotNull(name, "name");
        m_type = checkNotNull(type, "type");
        m_value = value;
        m_attributes = attributes;
    }

    public Timestamp getTimestamp() {
        return m_timestamp;
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
                "%s[timestamp=%s, resource=%s, name=%s, type=%s, value=%s]",
                getClass().getSimpleName(),
                getTimestamp(),
                getResource(),
                getName(),
                getType(),
                getValue());
    }

}
