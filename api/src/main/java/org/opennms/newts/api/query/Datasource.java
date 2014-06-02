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
package org.opennms.newts.api.query;


import static com.google.common.base.Preconditions.checkNotNull;

import org.opennms.newts.api.Duration;


public class Datasource {

    private final String m_label;
    private final String m_source;
    private final Duration m_heartbeat;
    private final double m_xff;
    private final AggregationFunction m_aggregationFunction;

    public Datasource(String label, String sourceName, Duration heartbeat, double xff, AggregationFunction aggregationFunction) {
        checkNotNull(label, "label argument");
        checkNotNull(sourceName, "source name argument");
        checkNotNull(heartbeat, "heartbeat argument");

        m_label = label;
        m_source = sourceName;
        m_heartbeat = heartbeat;
        m_xff = xff;
        m_aggregationFunction = aggregationFunction;
    }

    public String getLabel() {
        return m_label;
    }

    public String getSource() {
        return m_source;
    }

    public Duration getHeartbeat() {
        return m_heartbeat;
    }
    
    public double getXff() {
        return m_xff;
    }
    
    public AggregationFunction getAggregationFuction() {
        return m_aggregationFunction;
    }

    @Override
    public String toString() {
        return String.format(
                "%s[%s, source=%s, heatbeat=%s, xff=%s, function=%s]",
                getClass().getSimpleName(),
                getLabel(),
                getSource(),
                getHeartbeat(),
                getXff(),
                getAggregationFuction());
    }

}
