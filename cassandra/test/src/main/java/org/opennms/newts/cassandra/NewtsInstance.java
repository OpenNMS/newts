/*
 * Copyright 2016, The OpenNMS Group
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

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ServiceLoader;

import org.cassandraunit.MyCassandraCQLUnit;
import org.cassandraunit.dataset.CQLDataSet;
import org.cassandraunit.dataset.cql.FileCQLDataSet;
import org.cassandraunit.utils.EmbeddedCassandraServerHelper;
import org.junit.rules.ExternalResource;

import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import com.google.common.io.CharStreams;
import com.google.common.io.Files;

public class NewtsInstance extends ExternalResource {
    private static final String CASSANDRA_COMPRESSION = "NONE";
    private static final String CASSANDRA_KEYSPACE    = "newts";
    private static final String CASSANDRA_USERNAME = "cassandra";
    private static final String CASSANDRA_PASSWORD = "cassandra";

    private static final String KEYSPACE_PLACEHOLDER = "$KEYSPACE$";
    private static final String REPLICATION_FACTOR_PLACEHOLDER = "$REPLICATION_FACTOR$";

    private static ServiceLoader<Schema> schemaLoader = ServiceLoader.load(Schema.class);

    private MyCassandraCQLUnit cassandraUnit;
    private String host;
    private int port;

    @Override
    public void before() throws Throwable {
        cassandraUnit = new MyCassandraCQLUnit(getDataSet(CASSANDRA_KEYSPACE, 1));
        cassandraUnit.before();
        host = EmbeddedCassandraServerHelper.getHost();
        port = EmbeddedCassandraServerHelper.getNativeTransportPort();
    }

    @Override
    public void after() {
        cassandraUnit.after();
    }

    public CassandraSession getCassandraSession() {
        return new CassandraSessionImpl(CASSANDRA_KEYSPACE, host,
                port, CASSANDRA_COMPRESSION,
                CASSANDRA_USERNAME, CASSANDRA_PASSWORD, false);
    }

    public static CQLDataSet getDataSet(String keyspace, int replicationFactor) {
        try {
            //  Concatenate the schema strings
            String schemasString = "";
            for (Schema schema : schemaLoader) {
                schemasString += CharStreams.toString(new InputStreamReader(schema.getInputStream()));
            }

            // Replace the placeholders
            schemasString = schemasString.replace(KEYSPACE_PLACEHOLDER, keyspace);
            schemasString = schemasString.replace(REPLICATION_FACTOR_PLACEHOLDER, Integer.toString(replicationFactor));

            // Split the resulting script back into lines
            String lines[] = schemasString.split("\\r?\\n");

            // Remove duplicate CREATE KEYSPACE statements;
            StringBuffer sb = new StringBuffer();            
            boolean foundCreateKeyspace = false;
            boolean skipNextLine = false;
            for (String line : lines) {
                if (line.startsWith("CREATE KEYSPACE")) {
                    if (!foundCreateKeyspace) {
                        foundCreateKeyspace = true;
                        sb.append(line);
                        sb.append("\n");
                    } else {
                        skipNextLine = true;
                    }
                } else if (skipNextLine) {
                    skipNextLine = false;
                } else {
                    sb.append(line);
                    sb.append("\n");
                }
            }

            // Write the results to disk
            File schemaFile = File.createTempFile("schema-", ".cql", new File("target"));
            schemaFile.deleteOnExit();
            Files.write(sb.toString(), schemaFile, Charsets.UTF_8);
            return new FileCQLDataSet(schemaFile.getAbsolutePath(), false, true, keyspace);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getCompression() {
        return CASSANDRA_COMPRESSION;
    }

    public String getKeyspace() {
        return CASSANDRA_KEYSPACE;
    }

    public String getUsername() {
        return CASSANDRA_USERNAME;
    }

    public String getPassword() {
        return CASSANDRA_PASSWORD;
    }
}
