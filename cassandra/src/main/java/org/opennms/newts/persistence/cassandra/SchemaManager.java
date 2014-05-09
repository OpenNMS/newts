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
    private String m_keyspace;

    @Inject
    public SchemaManager(@Named("cassandraKeyspace") String keyspace, @Named("cassandraHost") String host, @Named("cassandraPort") int port) {
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
                try {
                    m_session.execute(statement.toString().replace("NEWTS_KEYSPACE", m_keyspace));
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
        String keyspace = System.getProperty("cassandraKeyspace", "newts");
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
            new SchemaManager(keyspace, hostname, portNumber).create(ifSchemaNotExists);
        }
        finally {
            System.exit(0);
        }

    }

}
