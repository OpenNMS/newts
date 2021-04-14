/*
 * Copyright 2016-2021, The OpenNMS Group
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


import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Pattern.Flag;

import com.fasterxml.jackson.annotation.JsonProperty;


public class CassandraConfig {

    @JsonProperty("keyspace")
    private String m_keyspace = "newts";

    @JsonProperty("host")
    private String m_host = "localhost";
    
    @JsonProperty("cloud-connect-bundle")
    private String m_cloudConnectBundle;

    @Min(value = 1024)
    @Max(value = 65535)
    @JsonProperty("port")
    private int m_port = 9042;

    @Min(value = 0)
    @JsonProperty("time-to-live")
    private int m_columnTTL = 31536000;
    
    @Pattern(regexp = "none|lzr|snappy", flags=Flag.CASE_INSENSITIVE)
    @JsonProperty("compression")
    private String m_compression = "NONE"; 

    @JsonProperty("username")
    private String m_username;

    @JsonProperty("password")
    private String m_password;

    @JsonProperty("ssl")
    private boolean m_ssl = false;

    @JsonProperty("core-connections-per-host")
    private Integer m_coreConnectionsPerHost;

    @JsonProperty("max-connections-per-host")
    private Integer m_maxConnectionsPerHost;

    @JsonProperty("max-requests-per-connection")
    private Integer m_maxRequestsPerConnection;

    public String getKeyspace() {
        return m_keyspace;
    }

    public String getHost() {
        return m_host;
    }

    public int getPort() {
        return m_port;
    }

    public int getColumnTTL() {
        return m_columnTTL;
    }

    public String getCompression() {
        return m_compression;
    }

    public String getUsername() {
        return m_username;
    }

    public String getPassword() {
        return m_password;
    }

    public boolean getSsl() {
        return m_ssl;
    }

    public Integer getCoreConnectionsPerHost() {
        return m_coreConnectionsPerHost;
    }

    public Integer getMaxConnectionsPerHost() {
        return m_maxConnectionsPerHost;
    }

    public Integer getMaxRequestsPerConnection() {
        return m_maxRequestsPerConnection;
    }
    
    public String getCloudConnectBundle() {
        return m_cloudConnectBundle;
    }
}
