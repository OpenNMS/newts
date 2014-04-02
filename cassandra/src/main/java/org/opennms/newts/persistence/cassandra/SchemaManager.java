package org.opennms.newts.persistence.cassandra;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.inject.Named;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.exceptions.AlreadyExistsException;
import com.google.inject.Inject;


public class SchemaManager implements AutoCloseable {

    private static final String SCHEMA_FILE = "schema.cql";

    private Cluster m_cluster;
    private Session m_session;

    @Inject
    public SchemaManager(@Named("cassandraHost") String host, @Named("cassandraPort") int port) {
        m_cluster = Cluster.builder().withPort(port).addContactPoint(host).build();
        m_session = m_cluster.connect();
    }

    public void create() throws IOException {
        create(true);
    }

    /**
     * Loads the schema from a file in classpath.
     *
     * @param ifNotExists
     *            ignore {@link AlreadyExistsException}s if true
     * @throws IOException
     */
    public void create(boolean ifNotExists) throws IOException {

        InputStream stream = getClass().getResourceAsStream("/" + SCHEMA_FILE);

        if (stream == null) {
            throw new RuntimeException(String.format("%s missing from classpath!", SCHEMA_FILE));
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

        String line, scrubbed;
        StringBuilder statement = new StringBuilder();

        while ((line = reader.readLine()) != null) {
            scrubbed = scrub(line);
            statement.append(scrubbed);

            if (scrubbed.endsWith(";")) {
                try {
                    m_session.execute(statement.toString());
                }
                catch (AlreadyExistsException e) {
                    if (ifNotExists) {
                        System.err.printf("%s (skipping)%n", e.getLocalizedMessage());
                    }
                    else {
                        throw e;
                    }
                }
                statement = new StringBuilder();
            }
        }

    }

    private String scrub(String line) {
        return line.replace("\\s+", "").replace("//.*$", "").replace(";.*$", ";");
    }

    @Override
    public void close() throws Exception {
        m_cluster.shutdown();
    }

    public static void main(String... args) throws IOException {

        String hostname = System.getProperty("cassandraHost", "localhost");
        String port = System.getProperty("cassandraPort", "9042");
        boolean ifSchemaNotExists = Boolean.valueOf(System.getProperty("ifSchemaNotExists", "true"));

        int portNumber = -1;

        try {
            portNumber = Integer.valueOf(port);
        }
        catch (NumberFormatException e) {
            System.err.printf("%s is an invalid port number", port);
            System.exit(1);
        }

        try {
            new SchemaManager(hostname, portNumber).create(ifSchemaNotExists);
        }
        finally {
            System.exit(0);
        }

    }

}
