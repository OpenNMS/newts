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


import java.util.Arrays;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;
import org.opennms.newts.api.Duration;
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
        private String m_heartbeat;

        public String getLabel() {
            return m_label;
        }

        public String getSource() {
            return m_source;
        }

        public StandardAggregationFunctions getFunction() {
            return m_function;
        }

        public Duration getHeartbeat() {
            return Duration.parse(m_heartbeat);
        }

        @Override
        public String toString() {
            return String.format(
                    "%s[label=%s, source=%s, function=%s, heartbeat=%s]",
                    getClass().getSimpleName(),
                    getLabel(),
                    getSource(),
                    getFunction(),
                    getHeartbeat());
        }

    }

    public static class Expression {

        @NotEmpty
        @JsonProperty("label")
        private String m_label;

        @NotEmpty
        @JsonProperty("expression")
        private String m_expression;

        public String getLabel() {
            return m_label;
        }

        public String getExpression() {
            return m_expression;
        }

        @Override
        public String toString() {
            return String.format(
                    "%s[label=%s, expression=%s]",
                    getClass().getSimpleName(),
                    getLabel());
        }
    }


    @JsonProperty("interval")
    private String m_interval = "300s";

    @Valid
    @JsonProperty("datasources")
    private Datasource[] m_datasources = {};
    
    @Valid
    @JsonProperty("expressions")
    private Expression[] m_expressions = {};

    @NotEmpty
    @JsonProperty("exports")
    private String[] m_exports;

    public Duration getInterval() {
        return Duration.parse(m_interval);
    }

    public Datasource[] getDatasources() {
        return m_datasources;
    }
    
    public Expression[] getExpressions() {
        return m_expressions;
    }

    public String[] getExports() {
        return m_exports;
    }

    @Override
    public String toString() {
        return String.format(
                "%s[interval=%s, datasources=%s, expressions=%s, exports=%s]",
                getClass().getSimpleName(),
                getInterval(),
                Arrays.asList(getDatasources()),
                Arrays.asList(getExpressions()),
                Arrays.asList(getExports()));
    }

}
