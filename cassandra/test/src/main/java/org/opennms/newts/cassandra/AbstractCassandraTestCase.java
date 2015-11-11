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


import java.io.File;
import java.io.IOException;

import org.cassandraunit.AbstractCassandraUnit4CQLTestCase;
import org.cassandraunit.dataset.CQLDataSet;
import org.cassandraunit.dataset.cql.FileCQLDataSet;

import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import com.google.common.io.Files;
import com.google.common.io.Resources;


public class AbstractCassandraTestCase extends AbstractCassandraUnit4CQLTestCase {

    protected static final String CASSANDRA_CONFIG      = "cassandra.yaml";
    protected static final String CASSANDRA_HOST        = "localhost";
    protected static final int    CASSANDRA_PORT        = 9043;
    protected static final String CASSANDRA_COMPRESSION = "NONE";
    protected static final String CASSANDRA_KEYSPACE    = "newts";
    public static final String CASSANDRA_USERNAME = "cassandra";
    public static final String CASSANDRA_PASSWORD = "cassandra";

    protected static final String KEYSPACE_PLACEHOLDER = "$KEYSPACE$";

    public AbstractCassandraTestCase() {
        super(CASSANDRA_CONFIG);
    }

    protected String getSchemaResource() {
        throw new UnsupportedOperationException();
    }

    @Override
    public CQLDataSet getDataSet() {
        try {
            String schema = Resources.toString(getClass().getResource(getSchemaResource()), Charsets.UTF_8);
            schema = schema.replace(KEYSPACE_PLACEHOLDER, CASSANDRA_KEYSPACE);
            File schemaFile = File.createTempFile("schema-", ".cql", new File("target"));
            Files.write(schema, schemaFile, Charsets.UTF_8);

            return new FileCQLDataSet(schemaFile.getAbsolutePath(), false, true, CASSANDRA_KEYSPACE);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

}
