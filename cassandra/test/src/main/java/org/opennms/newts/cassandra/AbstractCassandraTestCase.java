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

import org.cassandraunit.MyCassandraCQLUnit;
import org.cassandraunit.dataset.CQLDataSet;
import org.cassandraunit.dataset.cql.FileCQLDataSet;
import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.driver.core.CloseFuture;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import com.google.common.io.Files;
import com.google.common.io.Resources;


public abstract class AbstractCassandraTestCase {

    protected static final String CASSANDRA_CONFIG      = "cassandra.yaml";
    protected static final String CASSANDRA_HOST        = "localhost";
    protected static final int    CASSANDRA_PORT        = 9043;
    protected static final String CASSANDRA_COMPRESSION = "NONE";
    protected static final String CASSANDRA_KEYSPACE    = "newts";
    public static final String CASSANDRA_USERNAME = "cassandra";
    public static final String CASSANDRA_PASSWORD = "cassandra";

    protected static final String KEYSPACE_PLACEHOLDER = "$KEYSPACE$";

    private static final Logger log = LoggerFactory.getLogger(AbstractCassandraTestCase.class);

    private MyCassandraCQLUnit cassandraUnit;
    private boolean initialized = false;
    private Session session;
    private Cluster cluster;

    public AbstractCassandraTestCase() {
        cassandraUnit = new MyCassandraCQLUnit(getDataSet(), CASSANDRA_CONFIG);
    }


    @Before
    public void setUp() throws Exception {
        if (!initialized) {
            cassandraUnit.before();
            session = cassandraUnit.session;
            cluster = cassandraUnit.cluster;
            initialized = true;
        }
    }

    @After
    public void tearDown() throws Exception {
        if(session!=null){
            log.debug("session shutdown");
            CloseFuture closeFuture = session.closeAsync();
            closeFuture.force();
        }
        if (cluster != null) {
            log.debug("cluster shutdown");
            cluster.close();
        }
    }

    protected abstract String getSchemaResource();

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
