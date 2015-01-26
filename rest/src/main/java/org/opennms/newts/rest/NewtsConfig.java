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


import java.util.Collections;
import java.util.Map;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;


public class NewtsConfig extends Configuration {

    @Min(value = 1)
    @Max(value = 8192)
    @JsonProperty("maxSampleProcessorThreads")
    private int m_maxThreads = 64;

    @Valid
    @JsonProperty("search")
    private SearchConfig m_searchConfig = new SearchConfig();
    
    @Valid
    @JsonProperty("cassandra")
    private CassandraConfig m_cassandraConfig = new CassandraConfig();

    @Valid
    @JsonProperty("reports")
    private Map<String, ResultDescriptorDTO> m_reports = Collections.emptyMap();

    @Valid
    @JsonProperty("authentication")
    private AuthenticationConfig m_authenticationConfig = new AuthenticationConfig();

    public int getMaxSampleProcessorThreads() {
        return m_maxThreads;
    }

    public SearchConfig getSearchConfig() {
        return m_searchConfig;
    }

    public String getCassandraKeyspace() {
        return m_cassandraConfig.getKeyspace();
    }

    public String getCassandraHost() {
        return m_cassandraConfig.getHost();
    }

    public int getCassandraPort() {
        return m_cassandraConfig.getPort();
    }

    public int getCassandraColumnTTL() {
        return m_cassandraConfig.getColumnTTL();
    }

    public String getCassandraCompression() {
        return m_cassandraConfig.getCompression();
    }

    public Map<String, ResultDescriptorDTO> getReports() {
        return m_reports;
    }

    public AuthenticationConfig getAuthenticationConfig() {
        return m_authenticationConfig;
    }

}
