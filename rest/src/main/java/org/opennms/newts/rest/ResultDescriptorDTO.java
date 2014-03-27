package org.opennms.newts.rest;


import java.util.Arrays;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;
import org.opennms.newts.api.query.StandardAggregationFunctions;

import com.fasterxml.jackson.annotation.JsonProperty;


public class ResultDescriptorDTO {

    public static class Datasource {

        @NotEmpty
        @JsonProperty("label")
        private String m_label;

        @NotEmpty
        @JsonProperty("source")
        private String m_source;

        @NotNull
        @JsonProperty("function")
        private StandardAggregationFunctions m_function;

        @JsonProperty("heartbeat")
        private Integer m_heartbeat;

        public String getLabel() {
            return m_label;
        }

        public String getSource() {
            return m_source;
        }

        public StandardAggregationFunctions getFunction() {
            return m_function;
        }

        public Integer getHeartbeat() {
            return m_heartbeat;
        }

        @Override
        public String toString() {
            return String.format(
                    "%s[label=%s, source=%s, function=%s, heartbeat=%d]",
                    getClass().getSimpleName(),
                    getLabel(),
                    getSource(),
                    getFunction(),
                    getHeartbeat());
        }

    }

    public static class Calculation {

    }

    @Min(value = 1)
    @JsonProperty("interval")
    private int m_interval = 300;

    @Valid
    @JsonProperty("datasources")
    private Datasource[] m_datasources;

    @NotEmpty
    @JsonProperty("exports")
    private String[] m_exports;

    public int getInterval() {
        return m_interval;
    }

    public Datasource[] getDatasources() {
        return m_datasources;
    }

    public String[] getExports() {
        return m_exports;
    }

    @Override
    public String toString() {
        return String.format(
                "%s[interval=%d, datasources=%s, exports=%s]",
                getClass().getSimpleName(),
                getInterval(),
                Arrays.asList(getDatasources()),
                Arrays.asList(getExports()));
    }

}
