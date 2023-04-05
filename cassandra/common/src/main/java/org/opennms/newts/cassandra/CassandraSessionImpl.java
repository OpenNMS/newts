/*
 * Copyright 2023, The OpenNMS Group
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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import com.datastax.oss.driver.internal.core.config.typesafe.DefaultDriverConfigLoader;
import com.datastax.oss.driver.internal.core.ssl.DefaultSslEngineFactory;
import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.DriverException;
import com.datastax.oss.driver.api.core.config.DefaultDriverOption;
import com.datastax.oss.driver.api.core.config.DriverConfigLoader;
import com.datastax.oss.driver.api.core.config.ProgrammaticDriverConfigLoaderBuilder;
import com.datastax.oss.driver.api.core.cql.AsyncResultSet;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.datastax.oss.driver.api.core.cql.Statement;
import com.datastax.oss.driver.internal.core.connection.ExponentialReconnectionPolicy;

public class CassandraSessionImpl implements CassandraSession {

    private static final Logger LOG = LoggerFactory.getLogger(CassandraSessionImpl.class);

    private final CqlSession m_session;

    public CassandraSessionImpl(@Named("cassandra.datacenter") String datacenter,
                                @Named("cassandra.keyspace") String keyspace, @Named("cassandra.hostname") String hostname,
                                @Named("cassandra.port") int port, @Named("cassandra.compression") String compression,
                                @Named("cassandra.username") String username, @Named("cassandra.password") String password,
                                @Named("cassandra.ssl") boolean ssl) {
        this(datacenter, keyspace, hostname, port, compression, username, password, ssl, null, null, null);
    }

    public CassandraSessionImpl(@Named("cassandra.datacenter") String datacenter,
                                @Named("cassandra.keyspace") String keyspace, @Named("cassandra.hostname") String hostname,
                                @Named("cassandra.port") int port, @Named("cassandra.compression") String compression,
                                @Named("cassandra.username") String username, @Named("cassandra.password") String password,
                                @Named("cassandra.ssl") boolean ssl,
                                @Named("cassandra.driver-settings-file") String driverSettingsFile) {
        this(datacenter, keyspace, hostname, port, compression, username, password, ssl, null, null, driverSettingsFile);
    }

    @Inject
    public CassandraSessionImpl(@Named("cassandra.datacenter") String datacenter,
            @Named("cassandra.keyspace") String keyspace, @Named("cassandra.hostname") String hostname,
            @Named("cassandra.port") int port, @Named("cassandra.compression") String compression,
            @Named("cassandra.username") String username, @Named("cassandra.password") String password,
            @Named("cassandra.ssl") boolean ssl,
            @Named("cassandra.pool.connections-per-host") Integer connectionsPerHost,
            @Named("cassandra.pool.max-requests-per-connection") Integer maxRequestsPerConnection,
            @Named("cassandra.driver-settings-file") String driverSettingsFile) {

        if (!Strings.isNullOrEmpty(driverSettingsFile)) {
            File settingsFile = new File(driverSettingsFile);
            LOG.info("Setting up session with settings file: {}", settingsFile);
            m_session = CqlSession.builder()
                    .withConfigLoader(DefaultDriverConfigLoader.fromFile(settingsFile))
                    .withKeyspace(keyspace)
                    .build();
            return;
        }

        checkNotNull(datacenter, "datacenter argument");
        checkNotNull(keyspace, "keyspace argument");
        checkNotNull(hostname, "hostname argument");
        checkArgument(port > 0 && port < 65535, "not a valid port number: %d", port);
        checkNotNull(compression, "compression argument");

        LOG.info("Setting up session with to {}:{} using compression {} and local datacenter: {}", hostname, port,
                compression.toUpperCase(), datacenter);

        ProgrammaticDriverConfigLoaderBuilder configBuilder = DriverConfigLoader.programmaticBuilder()
                .startProfile("default");
        configBuilder.withString(DefaultDriverOption.SESSION_KEYSPACE, keyspace);
        configBuilder.withStringList(DefaultDriverOption.CONTACT_POINTS, toContactPoints(hostname, port));

        if (connectionsPerHost != null) {
            LOG.debug("Using {} connections per host.", connectionsPerHost);
            configBuilder.withInt(DefaultDriverOption.CONNECTION_POOL_LOCAL_SIZE, connectionsPerHost);
            configBuilder.withInt(DefaultDriverOption.CONNECTION_POOL_REMOTE_SIZE, connectionsPerHost);
        }
        if (maxRequestsPerConnection != null) {
            LOG.debug("Using {} max requests per connection.", maxRequestsPerConnection);
            configBuilder.withInt(DefaultDriverOption.CONNECTION_MAX_REQUESTS, maxRequestsPerConnection);
        }
        configBuilder.withClass(DefaultDriverOption.RECONNECTION_POLICY_CLASS, ExponentialReconnectionPolicy.class);
        configBuilder.withDuration(DefaultDriverOption.RECONNECTION_BASE_DELAY, Duration.of(1, ChronoUnit.SECONDS));
        configBuilder.withDuration(DefaultDriverOption.RECONNECTION_MAX_DELAY, Duration.of(2, ChronoUnit.MINUTES));
        configBuilder.withString(DefaultDriverOption.PROTOCOL_COMPRESSION, compression.toUpperCase());

        if (username != null && password != null) {
            LOG.info("Using username: {} and password: XXXXXXXX", username);
            configBuilder.withString(DefaultDriverOption.AUTH_PROVIDER_USER_NAME, username);
            configBuilder.withString(DefaultDriverOption.AUTH_PROVIDER_PASSWORD, password);
        }

        if (ssl) {
            configBuilder.withString(DefaultDriverOption.SSL_ENGINE_FACTORY_CLASS, DefaultSslEngineFactory.class.getCanonicalName());
        }

        m_session = CqlSession.builder()
                .withConfigLoader(configBuilder.build())
                .withLocalDatacenter(datacenter)
                .build();
    }

    protected static List<String> toContactPoints(String hostname, int port) {
        return Arrays.stream(hostname.split(","))
                .map(host -> {
                    if (host.indexOf(":") > 0) {
                        // use the specific port when one is set
                        return host;
                    } else {
                        // append the default port if none is set
                        return String.format("%s:%d", host, port);
                    }
                }).collect(Collectors.toList());
    }

    public PreparedStatement prepare(String statement) {
        try                           {  return m_session.prepare(statement);  }
        catch (DriverException excep) {  throw new CassandraException(excep);  }
    }

    public PreparedStatement prepare(SimpleStatement statement) {
        try                           {  return m_session.prepare(statement);  }
        catch (DriverException excep) {  throw new CassandraException(excep);  } 
    }

    public CompletionStage<AsyncResultSet> executeAsync(Statement statement) {
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

    public CompletionStage<Void> shutdown() {
        return m_session.closeAsync();
    }

}
