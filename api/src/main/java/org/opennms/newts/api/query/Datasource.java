package org.opennms.newts.api.query;


import org.opennms.newts.api.Duration;
import static com.google.common.base.Preconditions.checkNotNull;


public class Datasource {

    public Datasource(String name, String sourceName, Duration heartbeat) {
        checkNotNull(name, "name argument");
        checkNotNull(sourceName, "source name argument");
        checkNotNull(heartbeat, "heartbeat argument");
    }

}
