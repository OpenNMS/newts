package org.opennms.newts.search.cassandra.lucene;


import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;


class CassandraSession {

    private Session m_session;
    private final String m_indexName;

    CassandraSession(String keyspace, String hostname, int port, String indexName) {
        Cluster cluster = Cluster.builder().withPort(port).addContactPoint(hostname).build();

        m_session = cluster.connect(keyspace);
        m_indexName = indexName;
    }

    ResultSet execute(Statement statement) {
        return m_session.execute(statement);
    }

    String getIndexName() {
        return m_indexName;
    }

}
