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


import javax.validation.constraints.Min;

import com.fasterxml.jackson.annotation.JsonProperty;


public class SearchConfig {

    @JsonProperty("enabled")
    private boolean m_isEnabled = true;

    @Min(value = 0)
    @JsonProperty("maxCacheEntries")
    private long m_maxCacheEntries = 1000000;

    public boolean isEnabled() {
        return m_isEnabled;
    }

    public long getMaxCacheEntries() {
        return m_maxCacheEntries;
    }

}
