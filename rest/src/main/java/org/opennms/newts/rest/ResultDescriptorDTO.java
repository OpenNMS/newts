package org.opennms.newts.rest;


import org.opennms.newts.api.query.StandardAggregationFunctions;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;


public class ResultDescriptorDTO {

    public static class Datasource {
        private final String m_label;
        private final String m_source;
        private final StandardAggregationFunctions m_function;
        private final int m_heartbeat;

        @JsonCreator
        public Datasource(
                @JsonProperty("label") String label,
                @JsonProperty("source") String source,
                @JsonProperty("function") StandardAggregationFunctions function,
                @JsonProperty("heartbeat") Integer heartbeat) {

            m_label = label;
            m_source = source;
            m_function = function;
            m_heartbeat = heartbeat;

        }

        public String getLabel() {
            return m_label;
        }

        public String getSource() {
            return m_source;
        }

        public StandardAggregationFunctions getFunction() {
            return m_function;
        }

        public int getHeartbeat() {
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

    private final int m_interval;
    private final Datasource[] m_datasources;
    private final String[] m_exports;

    @JsonCreator
    public ResultDescriptorDTO(
            @JsonProperty("interval") Integer interval,
            @JsonProperty("datasources") Datasource[] datasources,
            @JsonProperty("exports") String[] exports) {

        m_interval = interval;
        m_datasources = datasources;
        m_exports = exports;

    }

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
                getDatasources(),
                getExports());
    }

}
