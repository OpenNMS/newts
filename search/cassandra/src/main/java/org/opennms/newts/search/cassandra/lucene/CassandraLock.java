package org.opennms.newts.search.cassandra.lucene;


import static com.datastax.driver.core.querybuilder.QueryBuilder.delete;
import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.insertInto;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.opennms.newts.search.cassandra.lucene.CassandraConstants.C_LOCKS_LOCKED;
import static org.opennms.newts.search.cassandra.lucene.CassandraConstants.C_LOCKS_NAME;
import static org.opennms.newts.search.cassandra.lucene.CassandraConstants.T_LOCKS;

import java.io.IOException;

import org.apache.lucene.store.Lock;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Statement;


public class CassandraLock extends Lock {

    private final CassandraSession m_session;
    private final String m_name;

    public CassandraLock(CassandraSession session, String name) {
        m_session = checkNotNull(session, "session argument");
        m_name = checkNotNull(name, "name argument");
    }

    @Override
    public boolean obtain() throws IOException {
        Statement query = insertInto(T_LOCKS).value(C_LOCKS_NAME, m_name).value(C_LOCKS_LOCKED, true).ifNotExists();
        ResultSet rs = m_session.execute(query);
        Row r = checkNotNull(rs.one(), "query (insert) result row");

        return r.getBool("[applied]");
    }

    @Override
    public void close() throws IOException {
        close(m_session, m_name);
    }

    static void close(CassandraSession session, String name) throws IOException {
        session.execute(delete().from(T_LOCKS).where(eq(C_LOCKS_NAME, name)));
    }

    @Override
    public boolean isLocked() throws IOException {
        Statement query = select().from(T_LOCKS).where(eq(C_LOCKS_NAME, m_name));
        ResultSet rs = m_session.execute(query);
        Row r = rs.one();

        return r != null ? r.getBool(C_LOCKS_LOCKED) : false;
    }

}
