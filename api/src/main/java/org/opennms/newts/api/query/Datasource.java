package org.opennms.newts.api.query;


import org.opennms.newts.api.Duration;
import static com.google.common.base.Preconditions.checkNotNull;


public class Datasource {

    private final String m_label;
    private final String m_source;
    private final Duration m_heartbeat;

    public Datasource(String label, String sourceName, Duration heartbeat) {
        checkNotNull(label, "label argument");
        checkNotNull(sourceName, "source name argument");
        checkNotNull(heartbeat, "heartbeat argument");

        m_label = label;
        m_source = sourceName;
        m_heartbeat = heartbeat;
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

}
