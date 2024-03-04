/*
 * Copyright 2014-2024, The OpenNMS Group
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


import static com.codahale.metrics.MetricRegistry.name;
import static com.datastax.oss.driver.api.querybuilder.QueryBuilder.bindMarker;
import static com.datastax.oss.driver.api.querybuilder.QueryBuilder.selectFrom;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;
import javax.inject.Named;

import org.opennms.newts.api.Context;
import org.opennms.newts.api.Resource;
import org.opennms.newts.api.search.BooleanClause;
import org.opennms.newts.api.search.BooleanQuery;
import org.opennms.newts.api.search.Query;
import org.opennms.newts.api.search.SearchResults;
import org.opennms.newts.api.search.Searcher;
import org.opennms.newts.api.search.TermQuery;
import org.opennms.newts.cassandra.CassandraSession;
import org.opennms.newts.cassandra.ContextConfigurations;
import org.opennms.newts.cassandra.search.Constants.Schema;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.datastax.oss.driver.api.core.ConsistencyLevel;
import com.datastax.oss.driver.api.core.cql.AsyncResultSet;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.querybuilder.select.Select;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class CassandraSearcher implements Searcher {

    private final CassandraSession m_session;
    private final Timer m_searchTimer;
    private final ContextConfigurations m_contextConfigurations;

    private final PreparedStatement m_searchStatement;
    private final PreparedStatement m_selectAttributesStatement;
    private final PreparedStatement m_selectMetricNamesStatement;

    @Inject
    public CassandraSearcher(CassandraSession session, @Named("newtsMetricRegistry") MetricRegistry registry, ContextConfigurations contextConfigurations) {
        m_session = checkNotNull(session, "session argument");
        m_searchTimer = registry.timer(name("search", "search"));
        m_contextConfigurations = checkNotNull(contextConfigurations, "contextConfigurations argument");

        Select select = selectFrom(Schema.T_TERMS).column(Schema.C_TERMS_RESOURCE)
                .whereColumn(Schema.C_TERMS_CONTEXT).isEqualTo(bindMarker(Schema.C_TERMS_CONTEXT))
               .whereColumn(Schema.C_TERMS_FIELD).isEqualTo(bindMarker(Schema.C_TERMS_FIELD))
               .whereColumn(Schema.C_TERMS_VALUE).isEqualTo(bindMarker(Schema.C_TERMS_VALUE));
        m_searchStatement = m_session.prepare(select.toString());

        select = selectFrom(Schema.T_ATTRS).columns(Schema.C_ATTRS_ATTR, Schema.C_ATTRS_VALUE)
                .whereColumn(Schema.C_ATTRS_CONTEXT).isEqualTo(bindMarker(Schema.C_ATTRS_CONTEXT))
                .whereColumn(Schema.C_ATTRS_RESOURCE).isEqualTo(bindMarker(Schema.C_ATTRS_RESOURCE));
        m_selectAttributesStatement = m_session.prepare(select.toString());

        select = selectFrom(Schema.T_METRICS).column(Schema.C_METRICS_NAME)
                .whereColumn(Schema.C_METRICS_CONTEXT).isEqualTo(bindMarker(Schema.C_METRICS_CONTEXT))
                .whereColumn(Schema.C_METRICS_RESOURCE).isEqualTo(bindMarker(Schema.C_METRICS_RESOURCE));
        m_selectMetricNamesStatement = m_session.prepare(select.toString());
    }

    @Override
    public SearchResults search(Context context, Query query) {
        return search(context, query, true);
    }

    @Override
    public SearchResults search(Context context, Query query, boolean populateMetricsAndAttributes) {
        checkNotNull(context, "context argument");
        checkNotNull(query, "query argument");

        Timer.Context ctx = m_searchTimer.time();
        ConsistencyLevel readConsistency = m_contextConfigurations.getReadConsistency(context);

        SearchResults searchResults = new SearchResults();

        try {
            Set<String> ids;
            Query q = query.rewrite();
            if (q instanceof BooleanQuery) {
                ids = searchForIds(context, (BooleanQuery)q, readConsistency);
            } else if (q instanceof TermQuery) {
                ids = searchForIds(context, (TermQuery)q, readConsistency);
            } else {
                throw new IllegalStateException("Unsupported query: " + q);
            }

            for (final String id : ids) {
                if (!populateMetricsAndAttributes) {
                    Resource resource = new Resource(id);
                    List<String> emptyList = Collections.emptyList();
                    searchResults.addResult(resource, emptyList);
                } else {
                    // Fetch the metric names and attributes concurrently
                    CompletionStage<AsyncResultSet> attrsFuture = fetchResourceAttributes(context, id, readConsistency);
                    CompletionStage<AsyncResultSet> metricsFuture = fetchMetricNames(context, id, readConsistency);

                    try {
                        Map<String, String> attrs = getResourceAttributesFromResults(attrsFuture);
                        Collection<String> metrics = getMetricNamesFromResults(metricsFuture);
                        Resource resource = attrs.size() > 0 ? new Resource(id, Optional.of(attrs)) : new Resource(id);
                        searchResults.addResult(resource, metrics);
                    } catch (ExecutionException|InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            return searchResults;
        }
        finally {
            ctx.stop();
        }
    }

    public Map<String, String> getResourceAttributes(Context context, String resourceId) {
        try {
            ConsistencyLevel readConsistency = m_contextConfigurations.getReadConsistency(context);
            return getResourceAttributesFromResults(fetchResourceAttributes(context, resourceId, readConsistency));
        } catch (ExecutionException|InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public Collection<String> getMetricNames(Context context, String resourceId) {
        try {
            ConsistencyLevel readConsistency = m_contextConfigurations.getReadConsistency(context);
            return getMetricNamesFromResults(fetchMetricNames(context, resourceId, readConsistency));
        } catch (ExecutionException|InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the set of resource ids that match the given
     * term query.
     */
    private Set<String> searchForIds(Context context, TermQuery query, ConsistencyLevel readConsistency) {
        Set<String> ids = Sets.newTreeSet();

        BoundStatement bindStatement = m_searchStatement.bind()
                .setString(Schema.C_TERMS_CONTEXT, context.getId())
                .setString(Schema.C_TERMS_FIELD, query.getTerm().getField(Constants.DEFAULT_TERM_FIELD))
                .setString(Schema.C_TERMS_VALUE, query.getTerm().getValue())
                .setConsistencyLevel(readConsistency);

        for (Row row : m_session.execute(bindStatement)) {
            ids.add(row.getString(Constants.Schema.C_TERMS_RESOURCE));
        }

        return ids;
    }

    /**
     * Returns the set of resource ids that match the given
     * boolean query.
     *
     * Separate clauses are performed with separate database queries and their
     * results are joined in memory.
     */
    private Set<String> searchForIds(Context context, BooleanQuery query, ConsistencyLevel readConsistency) {
        Set<String> ids = Sets.newTreeSet();

        for (BooleanClause clause : query.getClauses()) {
            Set<String> subQueryIds;

            Query subQuery = clause.getQuery();
            if (subQuery instanceof BooleanQuery) {
                subQueryIds = searchForIds(context, (BooleanQuery)subQuery, readConsistency);
            } else if (subQuery instanceof TermQuery) {
                subQueryIds = searchForIds(context, (TermQuery)subQuery, readConsistency);
            } else {
                throw new IllegalStateException("Unsupported query: " + subQuery);
            }

            switch (clause.getOperator()) {
                case AND: // Intersect
                    ids.retainAll(subQueryIds);
                    break;
                case OR: // Union
                    ids.addAll(subQueryIds);
                    break;
                default:
                    throw new IllegalStateException("Unsupported operator: " + clause.getOperator());
            }
        }

        return ids;
    }

    private CompletionStage<AsyncResultSet> fetchResourceAttributes(Context context, String resourceId, ConsistencyLevel readConsistency) {
        BoundStatement bindStatement = m_selectAttributesStatement.bind()
                .setString(Schema.C_ATTRS_CONTEXT, context.getId())
                .setString(Schema.C_ATTRS_RESOURCE, resourceId)
                .setConsistencyLevel(readConsistency);

        return m_session.executeAsync(bindStatement);
    }

    private Map<String, String> getResourceAttributesFromResults(CompletionStage<AsyncResultSet> results) throws InterruptedException, ExecutionException {
        Map<String, String> attributes = Maps.newHashMap();

        // blocking
        AsyncResultSet asyncResultSet = results.toCompletableFuture().get();
        while (true) {
            for (Row row : asyncResultSet.currentPage()) {
                attributes.put(row.getString(Schema.C_ATTRS_ATTR), row.getString(Schema.C_ATTRS_VALUE));
            }
            if (asyncResultSet.hasMorePages()) {
                // blocking
                asyncResultSet = asyncResultSet.fetchNextPage().toCompletableFuture().get();
            } else {
                break;
            }
        }

        return attributes;
    }

    private CompletionStage<AsyncResultSet> fetchMetricNames(Context context, String resourceId, ConsistencyLevel readConsistency) {
        BoundStatement bindStatement = m_selectMetricNamesStatement.bind()
                .setString(Schema.C_METRICS_CONTEXT, context.getId())
                .setString(Schema.C_METRICS_RESOURCE, resourceId)
                .setConsistencyLevel(readConsistency);
        return m_session.executeAsync(bindStatement);
    }

    private Collection<String> getMetricNamesFromResults(CompletionStage<AsyncResultSet> results) throws InterruptedException, ExecutionException {
        List<String> metricNames = Lists.newArrayList();

        // blocking
        AsyncResultSet asyncResultSet = results.toCompletableFuture().get();
        while (true) {
            for (Row row : asyncResultSet.currentPage()) {
                metricNames.add(row.getString(Schema.C_METRICS_NAME));
            }
            if (asyncResultSet.hasMorePages()) {
                // blocking
                asyncResultSet = asyncResultSet.fetchNextPage().toCompletableFuture().get();
            } else {
                break;
            }
        }

        return metricNames;
    }

}
