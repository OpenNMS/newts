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


import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.IOException;

import org.cassandraunit.AbstractCassandraUnit4CQLTestCase;
import org.cassandraunit.dataset.CQLDataSet;
import org.cassandraunit.dataset.cql.FileCQLDataSet;
import org.junit.After;
import org.junit.Before;
import org.opennms.newts.api.SampleProcessorService;
import org.opennms.newts.cassandra.CassandraSession;

import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import com.google.common.io.Files;
import com.google.common.io.Resources;


public class AbstractCassandraTestCase extends AbstractCassandraUnit4CQLTestCase {

    public static final String CASSANDRA_CONFIG = "cassandra.yaml";
    public static final String CASSANDRA_HOST = "localhost";
    public static final int CASSANDRA_PORT = 9043;
    public static final String CASSANDRA_COMPRESSION = "NONE";
    public static final int CASSANDRA_TTL = 86400;
    public static final String KEYSPACE_NAME = "newts";

    protected static final String KEYSPACE_PLACEHOLDER = "$KEYSPACE$";
    protected static final String SCHEMA_RESOURCE = "/samples_schema.cql";

    protected CassandraSampleRepository m_repository;

    public AbstractCassandraTestCase() {
        super(CASSANDRA_CONFIG, CASSANDRA_HOST, CASSANDRA_PORT);
    }

    @Before
    public void setUp() throws Exception {
        super.before();
        CassandraSession session = new CassandraSession(KEYSPACE_NAME, CASSANDRA_HOST, CASSANDRA_PORT, CASSANDRA_COMPRESSION);
        m_repository = new CassandraSampleRepository(
                session,
                CASSANDRA_TTL,
                new MetricRegistry(),
                mock(SampleProcessorService.class));
    }

    @After
    public void tearDown() throws Exception {
        super.after();
    }

    @Override
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
