/*
 * Copyright 2018, The OpenNMS Group
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

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;

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
        Select select = QueryBuilder.select(Constants.Schema.C_METRICS_CONTEXT,
                Constants.Schema.C_METRICS_RESOURCE,
                Constants.Schema.C_METRICS_NAME)
                .from(Constants.Schema.T_METRICS);
        m_selectAllMetricsStatement = session.prepare(select.toString());

        select = QueryBuilder.select(Constants.Schema.C_ATTRS_CONTEXT,
                Constants.Schema.C_ATTRS_RESOURCE,
                Constants.Schema.C_ATTRS_ATTR,
                Constants.Schema.C_ATTRS_VALUE)
                .from(Constants.Schema.T_ATTRS);
        m_selectAllAttributesStatement = session.prepare(select.toString());
    }

    public void prime(ResourceMetadataCache cache) {
        prime(cache, null);
    }

    public void prime(ResourceMetadataCache cache, Context context) {
        BoundStatement bindStatement = m_selectAllMetricsStatement.bind();
        bindStatement.setFetchSize(m_fetchSize);
        ResultSet rs = m_session.execute(bindStatement);
        for (Row row : rs) {
            // If we're nearing the end of the current page, trigger another fetch now instead
            // of waiting until all rows have been processed
            if (rs.getAvailableWithoutFetching() == m_fetchMoreThreshold && !rs.isFullyFetched()) {
                rs.fetchMoreResults(); // this is asynchronous
            }

            final Context rowContext = new Context(row.getString(Constants.Schema.C_METRICS_CONTEXT));
            if (context != null && !context.equals(rowContext)) {
                // Skip this entry, it's not in the context we're interested in
                continue;
            }

            final Resource resource = new Resource(row.getString(Constants.Schema.C_METRICS_RESOURCE));
            final ResourceMetadata resourceMetadata = new ResourceMetadata();
            resourceMetadata.putMetric(row.getString(Constants.Schema.C_METRICS_NAME));
            cache.merge(rowContext, resource, resourceMetadata);
        }

        bindStatement = m_selectAllAttributesStatement.bind();
        bindStatement.setFetchSize(m_fetchSize);
        for (Row row : m_session.execute(bindStatement)) {
            // If we're nearing the end of the current page, trigger another fetch now instead
            // of waiting until all rows have been processed
            if (rs.getAvailableWithoutFetching() == m_fetchMoreThreshold && !rs.isFullyFetched()) {
                rs.fetchMoreResults(); // this is asynchronous
            }

            final Context rowContext = new Context(row.getString(Constants.Schema.C_ATTRS_CONTEXT));
            if (context != null && !context.equals(rowContext)) {
                // Skip this entry, it's not in the context we're interested in
                continue;
            }

            final Resource resource = new Resource(row.getString(Constants.Schema.C_ATTRS_RESOURCE));
            final ResourceMetadata resourceMetadata = new ResourceMetadata();
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
