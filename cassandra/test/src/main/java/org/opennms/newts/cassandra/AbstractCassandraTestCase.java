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

    protected static final String KEYSPACE_PLACEHOLDER = "$KEYSPACE$";

    public AbstractCassandraTestCase() {
        super(CASSANDRA_CONFIG, CASSANDRA_HOST, CASSANDRA_PORT);
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
