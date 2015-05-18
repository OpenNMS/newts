package org.opennms.newts.cassandra.search;


import static com.codahale.metrics.MetricRegistry.name;
import static com.datastax.driver.core.querybuilder.QueryBuilder.bindMarker;
import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.opennms.newts.api.Context;
import org.opennms.newts.api.Resource;
import org.opennms.newts.api.search.BooleanClause;
import org.opennms.newts.api.search.BooleanQuery;
import org.opennms.newts.api.search.Query;
import org.opennms.newts.api.search.SearchResults;
import org.opennms.newts.api.search.Searcher;
import org.opennms.newts.api.search.TermQuery;
import org.opennms.newts.cassandra.CassandraSession;
import org.opennms.newts.cassandra.search.Constants.Schema;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.datastax.driver.core.querybuilder.Select.Where;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;


public class CassandraSearcher implements Searcher {

    private final CassandraSession m_session;
    private final Timer m_searchTimer;

    private final PreparedStatement m_selectAttributesStatement;
    private final PreparedStatement m_selectMetricNamesStatement;

    @Inject
    public CassandraSearcher(CassandraSession session, MetricRegistry registry) {
        m_session = checkNotNull(session, "session argument");
        m_searchTimer = registry.timer(name("search", "search"));

        Select select = QueryBuilder.select(Schema.C_ATTRS_ATTR, Schema.C_ATTRS_VALUE).from(Schema.T_ATTRS);
        select.where(eq(Schema.C_ATTRS_CONTEXT, bindMarker(Schema.C_ATTRS_CONTEXT)))
              .and(  eq(Schema.C_ATTRS_RESOURCE, bindMarker(Schema.C_ATTRS_RESOURCE)));
        m_selectAttributesStatement = m_session.prepare(select.toString());

        select = QueryBuilder.select(Schema.C_METRICS_NAME).from(Schema.T_METRICS);
        select.where(eq(Schema.C_METRICS_CONTEXT, bindMarker(Schema.C_METRICS_CONTEXT)))
              .and(  eq(Schema.C_METRICS_RESOURCE, bindMarker(Schema.C_METRICS_RESOURCE)));
        m_selectMetricNamesStatement = m_session.prepare(select.toString());
    }

    @Override
    public SearchResults search(Query query) {
        return search(query, Context.DEFAULT_CONTEXT);
    }

    private Set<String> searchForIds(TermQuery query, Context context) {
        Set<String> ids = Sets.newTreeSet();

        Where searchQuery = select(Constants.Schema.C_TERMS_RESOURCE).from(Constants.Schema.T_TERMS)
                .where(eq(Constants.Schema.C_TERMS_CONTEXT, Context.DEFAULT_CONTEXT.getId()))
                .and(  eq(Constants.Schema.C_TERMS_FIELD, query.getTerm().getField(Constants.DEFAULT_TERM_FIELD)))
                .and(  eq(Constants.Schema.C_TERMS_VALUE, query.getTerm().getValue()));

        for (Row row : m_session.execute(searchQuery)) {
            ids.add(row.getString(Constants.Schema.C_TERMS_RESOURCE));
        }

        return ids;
    }

    private Set<String> searchForIds(BooleanQuery query, Context context) {
        Set<String> ids = Sets.newTreeSet();

        for (BooleanClause clause : query.getClauses()) {
            Set<String> subQueryIds;

            Query subQuery = clause.getQuery();
            if (subQuery instanceof BooleanQuery) {
                subQueryIds = searchForIds((BooleanQuery)subQuery, context);
            } else if (subQuery instanceof TermQuery) {
                subQueryIds = searchForIds((TermQuery)subQuery, context);
            } else {
                throw new IllegalStateException("Unsupported query: " + subQuery);
            }

            switch(clause.getOperator()) {
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

    @Override
    public SearchResults search(Query query, Context context) {
        checkNotNull(query, "query argument");
        checkNotNull(context, "context argument");

        Timer.Context ctx = m_searchTimer.time();

        SearchResults searchResults = new SearchResults();

        Set<String> ids;
        Query q = query.rewrite();
        if (q instanceof BooleanQuery) {
            ids = searchForIds((BooleanQuery)q, context);
        } else if (q instanceof TermQuery){
            ids = searchForIds((TermQuery)q, context);
        } else {
            throw new IllegalStateException("Unsupported query: " + q);
        }

        // TODO: Use async DB calls; Get attrs and metrics concurrently
        for (final String id : ids) {
            Optional<Map<String, String>> attrs = fetchResourceAttributes(Context.DEFAULT_CONTEXT, id);
            Collection<String> metrics = fetchMetricNames(Context.DEFAULT_CONTEXT, id);

            searchResults.addResult(new Resource(id, attrs), metrics);
        }

        try {
            return searchResults;
        }
        finally {
            ctx.stop();
        }
    }

    private Optional<Map<String, String>> fetchResourceAttributes(Context context, String resourceId) {
        Map<String, String> attributes = Maps.newHashMap();

        BoundStatement bindStatement = m_selectAttributesStatement.bind();
        bindStatement.setString(Schema.C_ATTRS_CONTEXT, context.getId());
        bindStatement.setString(Schema.C_ATTRS_RESOURCE, resourceId);

        for (Row row : m_session.execute(bindStatement)) {
            attributes.put(row.getString(Schema.C_ATTRS_ATTR), row.getString(Schema.C_ATTRS_VALUE));
        }

        return attributes.size() > 0 ? Optional.of(attributes) : Optional.<Map<String, String>>absent();
    }

    private Collection<String> fetchMetricNames(Context context, String resourceId) {
        List<String> metricNames = Lists.newArrayList();

        BoundStatement bindStatement = m_selectMetricNamesStatement.bind();
        bindStatement.setString(Schema.C_METRICS_CONTEXT, context.getId());
        bindStatement.setString(Schema.C_METRICS_RESOURCE, resourceId);

        for (Row row : m_session.execute(bindStatement)) {
            metricNames.add(row.getString(Schema.C_METRICS_NAME));
        }

        return metricNames;
    }

}
