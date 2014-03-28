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
