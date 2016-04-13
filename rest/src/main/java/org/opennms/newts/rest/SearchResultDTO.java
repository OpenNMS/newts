/*
 * Copyright 2016, The OpenNMS Group
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

import java.util.Collection;

import javax.naming.directory.SearchResult;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;

/**
 * Read-only data transfer object for {@link SearchResult}s.
 * 
 * @author jwhite
 */
public class SearchResultDTO {

    private final ResourceDTO m_resource;
    private final Collection<String> m_metrics;

    @JsonCreator
    public SearchResultDTO(@JsonProperty("resource") ResourceDTO resource, @JsonProperty("metrics") Collection<String> metrics) {
        m_resource = checkNotNull(resource, "resource argument");
        m_metrics = ImmutableList.copyOf(checkNotNull(metrics, "metrics argument"));
    }

    public ResourceDTO getResource() {
        return m_resource;
    }

    public Collection<String> getMetrics() {
        return m_metrics;
    }
}
