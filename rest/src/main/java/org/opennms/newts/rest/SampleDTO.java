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
package org.opennms.newts.rest;


import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;

import org.opennms.newts.api.Context;
import org.opennms.newts.api.MetricType;
import org.opennms.newts.api.Sample;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;


/**
 * Read-only data transfer object for {@link Sample}s.
 * 
 * @author eevans
 */
@JsonSerialize(using=SampleDTOSerializer.class)
public class SampleDTO {

    private final long m_timestamp;
    private final ResourceDTO m_resource;
    private final String m_name;
    private final MetricType m_type;
    private final Number m_value;
    private final Map<String, String> m_attributes;
    private final Context m_context;

    @JsonCreator
    public SampleDTO(@JsonProperty("timestamp") long timestamp, @JsonProperty("resource") ResourceDTO resource,
            @JsonProperty("name") String name, @JsonProperty("type") MetricType type, @JsonProperty("value") Number value,
            @JsonProperty("attributes") Map<String, String> attributes, @JsonProperty("context") String context) {
        m_timestamp = checkNotNull(timestamp, "m_timestamp argument");
        m_resource = checkNotNull(resource, "m_resource argument");
        m_name = checkNotNull(name, "m_name argument");
        m_type = checkNotNull(type, "m_type argument");
        m_value = checkNotNull(value, "m_value argument");
        m_attributes = attributes;
        m_context = context != null ? new Context(context) : Context.DEFAULT_CONTEXT;
    }

    public long getTimestamp() {
        return m_timestamp;
    }

    public ResourceDTO getResource() {
        return m_resource;
    }

    public String getName() {
        return m_name;
    }

    public MetricType getType() {
        return m_type;
    }

    public Number getValue() {
        return m_value;
    }

    public Map<String, String> getAttributes() {
        return m_attributes;
    }

    public Context getContext() {
        return m_context;
    }
}
