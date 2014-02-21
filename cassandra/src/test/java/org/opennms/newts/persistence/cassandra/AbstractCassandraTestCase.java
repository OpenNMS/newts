package org.opennms.newts.persistence.cassandra;


import org.cassandraunit.AbstractCassandraUnit4CQLTestCase;
import org.cassandraunit.dataset.CQLDataSet;
import org.cassandraunit.dataset.cql.ClassPathCQLDataSet;
import org.junit.After;
import org.junit.Before;
import org.opennms.newts.api.SampleRepository;


public class AbstractCassandraTestCase extends AbstractCassandraUnit4CQLTestCase {

    public static final String CASSANDRA_CONFIG  = "cassandra.yaml";
    public static final String CASSANDRA_HOST    = "localhost";
    public static final int    CASSANDRA_PORT    = 9043;
    public static final String SCHEMA_FILE       = "schema.cql";
    public static final String KEYSPACE_NAME     = "newts";

    protected SampleRepository m_repository;

    public AbstractCassandraTestCase() {
        super(CASSANDRA_CONFIG, CASSANDRA_HOST, CASSANDRA_PORT);
    }

    @Before
    public void setUp() throws Exception {
        super.before();
        m_repository = new CassandraSampleRepository(KEYSPACE_NAME, CASSANDRA_HOST, CASSANDRA_PORT, null);
    }

    @After
    public void tearDown() throws Exception {
        super.after();
    }

    @Override
    public CQLDataSet getDataSet() {
        return new ClassPathCQLDataSet(SCHEMA_FILE, false, true, KEYSPACE_NAME);
    }

    public SampleRepository getRepository() {
        return m_repository;
    }

}
