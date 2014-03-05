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

}
