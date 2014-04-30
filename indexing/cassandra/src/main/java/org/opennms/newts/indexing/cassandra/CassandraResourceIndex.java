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
package org.opennms.newts.indexing.cassandra;

import static com.datastax.driver.core.querybuilder.QueryBuilder.batch;
import static com.datastax.driver.core.querybuilder.QueryBuilder.insertInto;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import javax.inject.Inject;
import javax.inject.Named;

import org.opennms.newts.api.indexing.ResourceIndex;
import org.opennms.newts.api.indexing.ResourcePath;

import com.codahale.metrics.MetricRegistry;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.Batch;
import com.google.common.collect.Multimap;


public class CassandraResourceIndex implements ResourceIndex {

    private static final String T_RESOURCE_IDX = "resource_idx";
    private static final String T_METRIC_IDX = "metric_idx";
    private static final String F_PARENT = "parent";
    private static final String F_CHILD = "child";
    private static final String F_RESOURCE = "resource";
    private static final String F_METRIC_NAME = "metric_name";
    private static final String DELIMITER = ":";

    private Session m_session;
    @SuppressWarnings("unused") private MetricRegistry m_registry;

    @Inject
    public CassandraResourceIndex(@Named("cassandra.keyspace") String keyspace, @Named("cassandra.host") String host, @Named("cassandra.port") int port, MetricRegistry registry) {
        checkNotNull(keyspace, "Cassandra keyspace argument");
        checkNotNull(host, "Cassandra host argument");
        checkArgument(port > 0, "invalid Cassandra port number: %s", port);

        Cluster cluster = Cluster.builder().withPort(port).addContactPoint(host).build();
        m_session = cluster.connect(keyspace);

        m_registry = checkNotNull(registry, "metric registry argument");
        
    }

    @Override
    public ResourcePath search(String... path) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String[] getMetrics(String resourceName) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void index(Multimap<String, String> metrics) {

        Batch batch = batch();

        for (String resource : metrics.keySet()) {
            // Whether the resource index needs updating
            boolean indexResource = true;

            // Create (or update), as necessary, each fully-qualified resource name / metric pair.
            for (String metric : metrics.get(resource)) {
                if (needsUpdate(resource, metric)) {
                    batch.add(insertInto(T_METRIC_IDX).value(F_RESOURCE, resource).value(F_METRIC_NAME, metric));
                }
                else {
                    // If needsUpdate() returns false even once, then there is no need to (re)index
                    // the resource.
                    indexResource = false;
                }
            }

            // Create (or update), as necessary, the resource path graph table.
            if (indexResource) {
                String[] paths = resource.split(DELIMITER);

                if (paths.length > 1) {
                    String parent = paths[0];

                    for (int i = 1; i < paths.length; i++) {
                        batch.add(insertInto(T_RESOURCE_IDX).value(F_PARENT, parent).value(F_CHILD, paths[i]));
                        parent = join(parent, paths[i]);
                    }
                }
            }
        }

        m_session.execute(batch);

    }

    private String join(String v1, String v2) {
        return String.format("%s%s%s", v1, DELIMITER, v2);
    }

    private boolean needsUpdate(String resource, String metric) {
        return true;
    }

}
