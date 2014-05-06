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


import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

import com.fasterxml.jackson.annotation.JsonProperty;


public class IndexingConfig {

    @JsonProperty("enabled")
    private boolean m_enabled = false;

    @Min(value = 1)
    @Max(value = 8192)
    @JsonProperty("maxThreads")
    private int m_maxThreads = 64;

    @Min(value = 10000)
    @Max(value = Integer.MAX_VALUE)
    @JsonProperty("maxCacheEntries")
    private int m_maxCacheEntries = 1000000;

    @Valid
    @JsonProperty("cassandra")
    private CassandraConfig m_cassandraConfig = new CassandraConfig();

    public boolean isEnabled() {
        return m_enabled;
    }

    public int getMaxThreads() {
        return m_maxThreads;
    }

    public String getCassandraKeyspace() {
        return m_cassandraConfig.getKeyspace();
    }

    public String getCassandraHost() {
        return m_cassandraConfig.getHost();
    }

    public int getMaxCacheEntries() {
        return m_maxCacheEntries;
    }

    public int getCassandraPort() {
        return m_cassandraConfig.getPort();
    }

    public int getCassandraColumnTTL() {
        return m_cassandraConfig.getColumnTTL();
    }

}
