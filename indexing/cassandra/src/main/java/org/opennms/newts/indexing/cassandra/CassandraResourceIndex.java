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
import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.insertInto;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.opennms.newts.api.indexing.ResourceIndex;
import org.opennms.newts.api.indexing.ResourcePath;

import com.codahale.metrics.MetricRegistry;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.Batch;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;


public class CassandraResourceIndex implements ResourceIndex {

    public static final String DELIMITER = ":";
    static final String ROOT_KEY = "<|> ROOT RESOURCE PATH <|>";

    private static final String T_RESOURCE_IDX = "resource_idx";
    private static final String T_METRIC_IDX = "metric_idx";
    private static final String F_PARENT = "parent";
    private static final String F_CHILD = "child";
    private static final String F_RESOURCE = "resource";
    private static final String F_METRIC_NAME = "metric_name";

    private Session m_session;
    private IndexState m_indexState;
    @SuppressWarnings("unused") private MetricRegistry m_registry;

    @Inject
    public CassandraResourceIndex(@Named("cassandra.keyspace") String keyspace, @Named("cassandra.host") String host, @Named("cassandra.port") int port,
            IndexState indexState, MetricRegistry registry) {
        checkNotNull(keyspace, "Cassandra keyspace argument");
        checkNotNull(host, "Cassandra host argument");
        checkArgument(port > 0, "invalid Cassandra port number: %s", port);

        Cluster cluster = Cluster.builder().withPort(port).addContactPoint(host).build();
        m_session = cluster.connect(keyspace);

        m_indexState = checkNotNull(indexState, "index state argument");
        m_registry = checkNotNull(registry, "metric registry argument");

    }

    Collection<String> getChildren(String parent) {

        List<String> children = Lists.newArrayList();
        Statement statement = select(F_CHILD).from(T_RESOURCE_IDX).where(eq(F_PARENT, parent));

        for (Row row : m_session.execute(statement)) {
            children.add(row.getString(F_CHILD));
        }

        return children;
    }

    @Override
    public ResourcePath search(String... path) {

        String key = (path.length > 0) ? path[0] : ROOT_KEY;
        CassandraResourcePath root = new CassandraResourcePath(key, this);

        for (int i = 1; i < path.length; i++) {
            root = new CassandraResourcePath(Optional.<ResourcePath> of(root), path[i], this);
        }

        return root;
    }

    @Override
    public Collection<String> getMetrics(String resourceName) {

        List<String> metrics = Lists.newArrayList();
        Statement statement = select(F_METRIC_NAME).from(T_METRIC_IDX).where(eq(F_RESOURCE, resourceName));

        for (Row row : m_session.execute(statement)) {
            metrics.add(row.getString(F_METRIC_NAME));
        }

        return metrics;
    }

    @Override
    public void index(Multimap<String, String> metrics) {

        Batch batch = batch();
        Multimap<String, String> needsUpdate = HashMultimap.create();

        for (String resource : metrics.keySet()) {
            // Whether the resource index needs updating
            boolean indexResource = true;

            // Create (or update), as necessary, each fully-qualified resource name / metric pair.
            for (String metric : metrics.get(resource)) {
                if (!m_indexState.exists(resource, metric)) {
                    batch.add(insertInto(T_METRIC_IDX).value(F_RESOURCE, resource).value(F_METRIC_NAME, metric));
                    needsUpdate.put(resource, metric);
                }
                else {
                    // If exists() returns true even once, then there is no need to (re)index
                    // the resource.
                    indexResource = false;
                }
            }

            // Create (or update), as necessary, the resource path graph table.
            if (indexResource) {
                String[] paths = resource.split(DELIMITER);

                if (paths.length > 1) {
                    String parent = paths[0];

                    batch.add(insertInto(T_RESOURCE_IDX).value(F_PARENT, ROOT_KEY).value(F_CHILD, parent));

                    for (int i = 1; i < paths.length; i++) {
                        batch.add(insertInto(T_RESOURCE_IDX).value(F_PARENT, parent).value(F_CHILD, paths[i]));
                        parent = join(parent, paths[i]);
                    }

                }
            }
        }

        m_session.execute(batch);

        // Add index changes to state only after the database update above succeeds
        m_indexState.putAll(needsUpdate);

    }

    private String join(String v1, String v2) {
        return Joiner.on(DELIMITER).join(v1, v2);
    }

}
