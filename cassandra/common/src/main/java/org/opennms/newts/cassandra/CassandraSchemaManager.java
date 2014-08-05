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
package org.opennms.newts.cassandra;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.inject.Named;
import javax.inject.Inject;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.exceptions.AlreadyExistsException;
import com.datastax.driver.core.exceptions.SyntaxError;


/**
 * Initialize Cassandra schema from an input file located on the classpath.
 * 
 * @author eevans
 */
public class CassandraSchemaManager implements AutoCloseable {

    private static final String KEYSPACE = "$KEYSPACE$";
    private static final String SCHEMA_FILE = "schema.cql";

    private String m_keyspace;
    private Cluster m_cluster;
    private Session m_session;

    @Inject
    public CassandraSchemaManager(@Named("cassandra.keyspace") String keyspace, @Named("cassandra.host") String host, @Named("cassandra.port") int port) {
        m_keyspace = keyspace;
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
                // Substitute the actual keyspace name for any KEYSPACE macros.
                String queryString = statement.toString().replace(KEYSPACE, m_keyspace);

                try {
                    m_session.execute(queryString);
                }
                catch (AlreadyExistsException e) {
                    if (ifNotExists) {
                        System.err.printf("%s (skipping)%n", e.getLocalizedMessage());
                    }
                    else {
                        throw e;
                    }
                }
                catch (SyntaxError e) {
                    System.out.printf("ERROR: %s (query: \"%s\").%n", e.getLocalizedMessage(), queryString);
                    throw e;
                }
                statement = new StringBuilder();
            }
        }

    }

    private static String scrub(String line) {
        return line.replace("\\s+", "").replace("//.*$", "").replace(";.*$", ";");
    }

    @Override
    public void close() throws Exception {
        m_cluster.shutdown();
    }

    public static void main(String... args) throws IOException {

        String keyspace = System.getProperty("cassandra.keyspace", "newts");
        String hostname = System.getProperty("cassandra.host", "localhost");
        String port = System.getProperty("cassandra.port", "9042");
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
            new CassandraSchemaManager(keyspace, hostname, portNumber).create(ifSchemaNotExists);
        }
        catch (Exception e) {
            System.exit(1);
        }

        System.exit(0);

    }

}
