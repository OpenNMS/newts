package org.opennms.newts.cassandra.search;


import static com.codahale.metrics.MetricRegistry.name;
import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.opennms.newts.api.Context;
import org.opennms.newts.api.Resource;
import org.opennms.newts.api.search.SearchResults;
import org.opennms.newts.api.search.Searcher;
import org.opennms.newts.cassandra.CassandraSession;
import org.opennms.newts.cassandra.search.Constants.Schema;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Statement;
import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;


// FIXME: Needs proper query string parser; half-assedness ahead
public class CassandraSearcher implements Searcher {

    private final static Splitter s_tokenSplitter = Splitter.onPattern("\\s+").omitEmptyStrings().trimResults();

    private final CassandraSession m_session;
    private final Timer m_searchTimer;

    @Inject
    public CassandraSearcher(CassandraSession session, MetricRegistry registry) {
        m_session = checkNotNull(session, "session argument");
        m_searchTimer = registry.timer(name("search", "search"));
    }

    // FIXME: use of hard-coded application ID!
    public SearchResults search(String queryString) {

        Timer.Context ctx = m_searchTimer.time();

        SearchResults searchResults = new SearchResults();

        for (String term : s_tokenSplitter.splitToList(queryString)) {

            Term t = Term.parse(term);

            Statement searchQuery = select(Constants.Schema.C_TERMS_RESOURCE).from(Constants.Schema.T_TERMS)
                    .where(eq(Constants.Schema.C_TERMS_CONTEXT, Context.DEFAULT_CONTEXT.getId()))
                    .and(  eq(Constants.Schema.C_TERMS_FIELD, t.getField()))
                    .and(  eq(Constants.Schema.C_TERMS_VALUE, t.getValue()));

            // TODO: Use async DB calls; Get attrs and metrics concurrently
            for (Row row : m_session.execute(searchQuery.toString())) {  // FIXME: toString()?
                String id = row.getString(Constants.Schema.C_TERMS_RESOURCE);
                Optional<Map<String, String>> attrs = fetchResourceAttributes(Context.DEFAULT_CONTEXT, id);
                Collection<String> metrics = fetchMetricNames(Context.DEFAULT_CONTEXT, id);

                searchResults.addResult(new Resource(id, attrs), metrics);
            }
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

        // TODO: Use prepared statement.
        Statement searchQuery = select(Schema.C_ATTRS_ATTR, Schema.C_ATTRS_VALUE).from(Constants.Schema.T_ATTRS)
                .where(eq(Schema.C_ATTRS_CONTEXT, context.getId()))
                .and(  eq(Schema.C_ATTRS_RESOURCE, resourceId));

        for (Row row : m_session.execute(searchQuery.toString())) {      // FIXME: toString()?
            attributes.put(row.getString(Schema.C_ATTRS_ATTR), row.getString(Schema.C_ATTRS_VALUE));
        }

        return attributes.size() > 0 ? Optional.of(attributes) : Optional.<Map<String, String>>absent();
    }

    private Collection<String> fetchMetricNames(Context context, String resourceId) {
        List<String> metricNames = Lists.newArrayList();

        // TODO: Use prepared statement.
        Statement select = select(Schema.C_METRICS_NAME).from(Schema.T_METRICS)
                .where(eq(Schema.C_METRICS_CONTEXT, context.getId()))
                .and(  eq(Schema.C_METRICS_RESOURCE, resourceId));

        for (Row row : m_session.execute(select.toString())) {  // FIXME: toString()?
            metricNames.add(row.getString(Schema.C_METRICS_NAME));
        }

        return metricNames;
    }

    static class Term {

        private static Splitter s_splitter = Splitter.on(':').limit(2).trimResults().omitEmptyStrings();

        private String m_field;
        private String m_value;

        Term(String field, String value) {
            m_field = checkNotNull(field, "field argument");
            m_value = checkNotNull(value, "value argument");
        }

        String getField() {
            return m_field;
        }

        String getValue() {
            return m_value;
        }

        static Term parse(String term) {
            List<String> t = s_splitter.splitToList(term);

            String field = t.size() < 2 ? Constants.DEFAULT_TERM_FIELD : t.get(0);
            String value = t.size() < 2 ? t.get(0) : t.get(1);

            return new Term(field, value);
        }

    }

}
