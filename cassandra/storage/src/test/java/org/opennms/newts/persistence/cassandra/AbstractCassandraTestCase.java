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
package org.opennms.newts.persistence.cassandra;


import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.IOException;

import org.cassandraunit.CQLDataLoader;
import org.cassandraunit.MyCassandraCQLUnit;
import org.cassandraunit.dataset.CQLDataSet;
import org.cassandraunit.dataset.cql.FileCQLDataSet;
import org.junit.After;
import org.junit.Before;
import org.opennms.newts.api.SampleProcessorService;
import org.opennms.newts.cassandra.CassandraSession;
import org.opennms.newts.cassandra.ContextConfigurations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.MetricRegistry;
import com.datastax.driver.core.CloseFuture;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import com.google.common.io.Files;
import com.google.common.io.Resources;


public class AbstractCassandraTestCase {

    public static final String CASSANDRA_CONFIG = "cassandra.yaml";
    public static final String CASSANDRA_HOST = "localhost";
    public static final int CASSANDRA_PORT = 9043;
    public static final String CASSANDRA_COMPRESSION = "NONE";
    public static final String CASSANDRA_USERNAME = "cassandra";
    public static final String CASSANDRA_PASSWORD = "cassandra";
    public static final int CASSANDRA_TTL = 86400;
    public static final String KEYSPACE_NAME = "newts";

    protected static final String KEYSPACE_PLACEHOLDER = "$KEYSPACE$";
    protected static final String SCHEMA_RESOURCE = "/samples_schema.cql";

    protected CassandraSampleRepository m_repository;
    protected ContextConfigurations m_contextConfigurations = new ContextConfigurations();


    private static final Logger log = LoggerFactory.getLogger(CQLDataLoader.class);

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

        CassandraSession session = new CassandraSession(KEYSPACE_NAME, CASSANDRA_HOST,
                CASSANDRA_PORT, CASSANDRA_COMPRESSION,
                CASSANDRA_USERNAME, CASSANDRA_PASSWORD);
        m_repository = new CassandraSampleRepository(
                session,
                CASSANDRA_TTL,
                new MetricRegistry(),
                mock(SampleProcessorService.class),
                m_contextConfigurations);
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

    public CQLDataSet getDataSet() {
        try {
            String schema = Resources.toString(getClass().getResource(SCHEMA_RESOURCE), Charsets.UTF_8);
            schema = schema.replace(KEYSPACE_PLACEHOLDER, KEYSPACE_NAME);
            File schemaFile = File.createTempFile("schema-", ".cql", new File("target"));
            Files.write(schema, schemaFile, Charsets.UTF_8);
            
            return new FileCQLDataSet(schemaFile.getAbsolutePath(), false, true, KEYSPACE_NAME);
            
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
        
    }

    public CassandraSampleRepository getRepository() {
        return m_repository;
    }
}
