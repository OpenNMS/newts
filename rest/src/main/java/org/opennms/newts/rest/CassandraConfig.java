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

}
