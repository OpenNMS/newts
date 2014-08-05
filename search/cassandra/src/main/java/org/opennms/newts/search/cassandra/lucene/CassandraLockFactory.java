package org.opennms.newts.search.cassandra.lucene;


import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;

import org.apache.lucene.store.Lock;
import org.apache.lucene.store.LockFactory;


public class CassandraLockFactory extends LockFactory {

    private final CassandraSession m_session;

    public CassandraLockFactory(CassandraSession session) {
        m_session = checkNotNull(session, "session argument");
    }

    @Override
    public Lock makeLock(String lockName) {
        return new CassandraLock(m_session, lockName);
    }

    @Override
    public void clearLock(String lockName) throws IOException {
        CassandraLock.close(m_session, lockName);
    }

}
