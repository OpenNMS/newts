package org.opennms.newts.api.query;


import org.opennms.newts.api.Duration;
import static com.google.common.base.Preconditions.checkNotNull;


public class Datasource {

    private final String m_name;
    private final String m_source;
    private final Duration m_heartbeat;

    public Datasource(String name, String sourceName, Duration heartbeat) {
        checkNotNull(name, "name argument");
        checkNotNull(sourceName, "source name argument");
        checkNotNull(heartbeat, "heartbeat argument");

        m_name = name;
        m_source = sourceName;
        m_heartbeat = heartbeat;
    }

    public String getName() {
        return m_name;
    }

    public String getSource() {
        return m_source;
    }

    public Duration getHeartbeat() {
        return m_heartbeat;
    }

}
