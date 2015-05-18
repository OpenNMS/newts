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

import java.util.Collections;
import java.util.Map;

import org.opennms.newts.api.Resource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * Read-only data transfer object for {@link Resource}s
 * 
 * @author eevans
 */
public class ResourceDTO {

    private final String m_id;
    private final Map<String, String> m_attributes;

    @JsonCreator
    public ResourceDTO(@JsonProperty("id") String id, @JsonProperty("attributes") Map<String, String> attributes) {
        m_id = checkNotNull(id, "id argument");
        m_attributes = attributes == null ? Collections.<String, String>emptyMap() : attributes;
    }

    public String getId() {
        return m_id;
    }

    public Map<String, String> getAttributes() {
        return m_attributes;
    }

}
