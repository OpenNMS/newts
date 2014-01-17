package org.opennms.newts;


import static com.google.common.base.Preconditions.checkArgument;

import java.net.URI;

import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.impl.DefaultEndpoint;


/**
 * Represents a Newts endpoint.
 */
public class NewtsEndpoint extends DefaultEndpoint {

    private final String m_hostname;
    private final int m_port;
    private final String m_keyspace;

    public NewtsEndpoint(String uri, NewtsComponent component) {
        super(uri, component);

        URI u = URI.create(getEndpointUri());

        m_hostname = (u.getHost() == null) ? "localhost" : u.getHost();
        m_port = (u.getPort() < 0) ? 9042 : u.getPort();
        m_keyspace = u.getPath().startsWith("/") ? u.getPath().substring(1) : u.getPath();

        checkArgument(m_keyspace.matches("^[A-Za-z]\\w+"), String.format("Invalid keyspace name: \"%s\"", m_keyspace));

    }

    public Producer createProducer() throws Exception {
        return new NewtsProducer(this);
    }

    public Consumer createConsumer(Processor processor) throws Exception {
        throw new UnsupportedOperationException("Newts consumer is not implemented");
    }

    public boolean isSingleton() {
        return true;
    }

    public String getHostname() {
        return m_hostname;
    }

    public int getPort() {
        return m_port;
    }

    public String getKeyspace() {
        return m_keyspace;
    }

}
