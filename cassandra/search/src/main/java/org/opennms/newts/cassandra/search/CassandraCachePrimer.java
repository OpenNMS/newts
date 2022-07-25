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
package org.opennms.newts.cassandra.search;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.inject.Inject;

import org.opennms.newts.api.Context;
import org.opennms.newts.api.Resource;
import org.opennms.newts.cassandra.CassandraSession;

import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.querybuilder.QueryBuilder;
import com.datastax.oss.driver.api.querybuilder.select.Select;

/**
 * NOTE: Due to the current schema, we cannot perform server side filtering
 * of the contexts.
 */
public class CassandraCachePrimer {
    public static final int DEFAULT_FETCH_SIZE = 10000;
    public static final int DEFAULT_FETCH_MORE_THRESHOLD = 1000;

    private final CassandraSession m_session;

    private final PreparedStatement m_selectAllMetricsStatement;
    private final PreparedStatement m_selectAllAttributesStatement;

    private int m_fetchSize = DEFAULT_FETCH_SIZE;
    private int m_fetchMoreThreshold = DEFAULT_FETCH_MORE_THRESHOLD;

    @Inject
    public CassandraCachePrimer(CassandraSession session) {
        m_session = checkNotNull(session);
        Select select = QueryBuilder.selectFrom(Constants.Schema.T_METRICS)
                .column(Constants.Schema.C_METRICS_CONTEXT)
                .column(Constants.Schema.C_METRICS_RESOURCE)
                .column(Constants.Schema.C_METRICS_NAME);
        m_selectAllMetricsStatement = session.prepare(select.toString());

        select = QueryBuilder.selectFrom(Constants.Schema.T_ATTRS)
                .column(Constants.Schema.C_ATTRS_CONTEXT)
                .column(Constants.Schema.C_ATTRS_RESOURCE)
                .column(Constants.Schema.C_ATTRS_ATTR)
                .column(Constants.Schema.C_ATTRS_VALUE)
                .ttl(Constants.Schema.C_ATTRS_VALUE).as("ttl");
        m_selectAllAttributesStatement = session.prepare(select.toString());
    }

    public void prime(ResourceMetadataCache cache) {
        prime(cache, null);
    }

    public void prime(ResourceMetadataCache cache, Context context) {
        BoundStatement bindStatement = m_selectAllMetricsStatement.bind()
                .setPageSize(m_fetchSize);
        ResultSet rs = m_session.execute(bindStatement);
        for (Row row : rs) {
            final Context rowContext = new Context(row.getString(Constants.Schema.C_METRICS_CONTEXT));
            if (context != null && !context.equals(rowContext)) {
                // Skip this entry, it's not in the context we're interested in
                continue;
            }

            final Resource resource = new Resource(row.getString(Constants.Schema.C_METRICS_RESOURCE));
            final ResourceMetadata resourceMetadata = new ResourceMetadata();

            // As cassandra is unable to provide remaining TTL values for tables with partition/primary keys only, we
            // hope that the cached metadata is merged with one from the `resource_attributes` table on which the TTL
            // can be determined. As a fallback, we let the cache expire immediately avoiding objects in the cache with
            // are in fact already timed out.
            resourceMetadata.setExpires(null);

            resourceMetadata.putMetric(row.getString(Constants.Schema.C_METRICS_NAME));
            cache.merge(rowContext, resource, resourceMetadata);
        }

        bindStatement = m_selectAllAttributesStatement.bind()
                .setPageSize(m_fetchSize);
        for (Row row : m_session.execute(bindStatement)) {
            final Context rowContext = new Context(row.getString(Constants.Schema.C_ATTRS_CONTEXT));
            if (context != null && !context.equals(rowContext)) {
                // Skip this entry, it's not in the context we're interested in
                continue;
            }

            final Resource resource = new Resource(row.getString(Constants.Schema.C_ATTRS_RESOURCE));
            final ResourceMetadata resourceMetadata = new ResourceMetadata();

            // Let the caches expire before the real TTL to avoid corner-cases and add some margin
            // Cassandra supports fetching of TTL values only on rows where not all columns are primary keys. Therefore
            // we assume that the TTL of entries in this table is similar to entries of other metadata tables. Setting
            // the expiration time only once will merge the value to all other cached entries for the same resource
            resourceMetadata.setExpires(System.currentTimeMillis() + row.getInt("ttl") * 1000L * 3L / 4L);

            resourceMetadata.putAttribute(row.getString(Constants.Schema.C_ATTRS_ATTR),
                    row.getString(Constants.Schema.C_ATTRS_VALUE));
            cache.merge(rowContext, resource, resourceMetadata);
        }
    }

    public int getFetchSize() {
        return m_fetchSize;
    }

    public void setFetchSize(int fetchSize) {
        m_fetchSize = fetchSize;
    }

    public int getFetchMoreThreshold() {
        return m_fetchMoreThreshold;
    }

    public void setFetchMoreThreshold(int fetchMoreThreshold) {
        m_fetchMoreThreshold = fetchMoreThreshold;
    }
}
