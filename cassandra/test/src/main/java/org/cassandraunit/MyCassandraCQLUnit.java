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
package org.cassandraunit;

import org.cassandraunit.dataset.CQLDataSet;
import org.cassandraunit.utils.EmbeddedCassandraServerHelper;

import com.datastax.driver.core.Cluster;

/**
 * We currently use a newer driver than the one associated with the
 * cassandra-unit package and need to override the load() method
 * to make things work.
 *
 * This shouldn't be necessary when upgrading to cassandra-unit gt 3.0.0
 *
 * @author jwhite
 */
public class MyCassandraCQLUnit extends CassandraCQLUnit {
    private final CQLDataSet dataSet;

    public MyCassandraCQLUnit(CQLDataSet dataSet, String configurationFileName) {
        super(dataSet, configurationFileName);
        this.dataSet = dataSet;
    }

    @Override
    protected void load() {
        String hostIp = EmbeddedCassandraServerHelper.getHost();
        int port = EmbeddedCassandraServerHelper.getNativeTransportPort();
        cluster = new Cluster.Builder().addContactPoint(hostIp).withPort(port).build();
        session = cluster.connect();
        CQLDataLoader dataLoader = new CQLDataLoader(session);
        dataLoader.load(dataSet);
        session = dataLoader.getSession();
    }

    public void before() throws Exception {
        super.before();
    }
}
