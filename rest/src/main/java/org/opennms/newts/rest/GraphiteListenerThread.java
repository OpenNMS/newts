package org.opennms.newts.rest;

import org.opennms.newts.graphite.GraphiteListener;

/**
 * Daemon thread for a Graphite line-protocol listener.
 * 
 * @author eevans
 *
 */
public class GraphiteListenerThread extends Thread {
    public GraphiteListenerThread(GraphiteListener listener) {
        super(listener);
        setDaemon(true);
        setName("GRAPHITE-LISTENER");
    }
}
