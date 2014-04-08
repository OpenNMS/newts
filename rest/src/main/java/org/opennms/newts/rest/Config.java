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
import com.yammer.dropwizard.config.Configuration;


public class Config extends Configuration {

    @JsonProperty("cassandra.keyspace")
    private String m_cassandraKeyspace = "newts";

    @JsonProperty("cassandra.host")
    private String m_cassandraHost = "localhost";

    @Min(value = 1024)
    @Max(value = 65535)
    @JsonProperty("cassandra.port")
    private int m_cassandraPort = 9042;

    @Valid
    @JsonProperty("reports")
    private Map<String, ResultDescriptorDTO> m_reports = Collections.emptyMap();

    public String getCassandraKeyspace() {
        return m_cassandraKeyspace;
    }

    public String getCassandraHost() {
        return m_cassandraHost;
    }

    public int getCassandraPort() {
        return m_cassandraPort;
    }

    public Map<String, ResultDescriptorDTO> getReports() {
        return m_reports;
    }

}
