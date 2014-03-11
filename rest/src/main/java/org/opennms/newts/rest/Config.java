package org.opennms.newts.rest;


import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.yammer.dropwizard.config.Configuration;


public class Config extends Configuration {

    @NotEmpty
    @JsonProperty
    private String cassandraKeyspace;

    @NotEmpty
    @JsonProperty
    private String cassandraHost;

    @NotNull
    @JsonProperty
    private int cassandraPort;

    public String getCassandraKeyspace() {
        return cassandraKeyspace;
    }

    public String getCassandraHost() {
        return cassandraHost;
    }

    public int getCassandraPort() {
        return cassandraPort;
    }

}
