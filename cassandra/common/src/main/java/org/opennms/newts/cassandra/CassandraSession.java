package org.opennms.newts.cassandra;


import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.driver.core.CloseFuture;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ProtocolOptions.Compression;
import com.datastax.driver.core.RegularStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.exceptions.DriverException;


public class CassandraSession {

    private static final Logger LOG = LoggerFactory.getLogger(CassandraSession.class);

    private final Session m_session;

    @Inject
    public CassandraSession(@Named("cassandra.keyspace") String keyspace, @Named("cassandra.hostname") String hostname, @Named("cassandra.port") int port, @Named("cassandra.compression") String compression) {
        checkNotNull(keyspace, "keyspace argument");
        checkNotNull(hostname, "hostname argument");
        checkArgument(port > 0 && port < 65535, "not a valid port number: %d", port);
        checkNotNull(compression, "compression argument");

        LOG.info("Setting up session with {}:{} using compression {}", hostname, port, compression.toUpperCase());

        Cluster cluster = Cluster
                .builder()
                .withPort(port)
                .addContactPoint(hostname)
                .withCompression(Compression.valueOf(compression.toUpperCase()))
                .build();

        m_session = cluster.connect(keyspace);

    }

    public PreparedStatement prepare(String statement) {
        try                           {  return m_session.prepare(statement);  }
        catch (DriverException excep) {  throw new CassandraException(excep);  } 
    }

    public PreparedStatement prepare(RegularStatement statement) {
        try                           {  return m_session.prepare(statement);  }
        catch (DriverException excep) {  throw new CassandraException(excep);  } 
    }

    public ResultSetFuture executeAsync(Statement statement) {
        try                           {  return m_session.executeAsync(statement);  }
        catch (DriverException excep) {  throw new CassandraException(excep);  } 
    }

    public ResultSet execute(Statement statement) {
        try                           {  return m_session.execute(statement);  }
        catch (DriverException excep) {  throw new CassandraException(excep);  }
    }

    public ResultSet execute(String statement) {
        try                           {  return m_session.execute(statement);  }
        catch (DriverException excep) {  throw new CassandraException(excep);  }
    }

    public Future<Void> shutdown() {
        final CloseFuture future = m_session.closeAsync();

        return new Future<Void>() {

            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                return future.cancel(mayInterruptIfRunning);
            }

            @Override
            public boolean isCancelled() {
                return future.isCancelled();
            }

            @Override
            public boolean isDone() {
                return future.isDone();
            }

            @Override
            public Void get() throws InterruptedException, ExecutionException {
                return future.get();
            }

            @Override
            public Void get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                return future.get(timeout, unit);
            }
        };

    }

}
