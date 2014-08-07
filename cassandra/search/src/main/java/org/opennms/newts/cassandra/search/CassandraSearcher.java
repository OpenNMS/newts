package org.opennms.newts.cassandra.search;


import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.opennms.newts.api.Resource;
import org.opennms.newts.cassandra.CassandraSession;
import org.opennms.newts.cassandra.search.Constants.Schema;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Statement;
import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;


public class CassandraSearcher {

    private CassandraSession m_session;

    @Inject
    public CassandraSearcher(CassandraSession session) {
        m_session = checkNotNull(session, "session argument");
    }

    private Optional<Map<String, String>> fetchResourceAttributes(String appName, String resourceId) {
        Map<String, String> attributes = Maps.newHashMap();

        Statement searchQuery = select(Schema.C_ATTRS_ATTR, Schema.C_ATTRS_VALUE).from(Constants.Schema.T_ATTRS)
                .where(eq(Schema.C_ATTRS_APP, appName))
                .and(  eq(Schema.C_ATTRS_RESOURCE, resourceId));

        ResultSet rs = m_session.execute(searchQuery.toString()); // FIXME: toString()?

        for (Row row : rs) {
            attributes.put(row.getString(Schema.C_ATTRS_ATTR), row.getString(Schema.C_ATTRS_VALUE));
        }

        return attributes.size() > 0 ? Optional.of(attributes) : Optional.<Map<String, String>>absent();
    }

    // FIXME: hard-coded application ID!
    public Collection<Resource> search(String... terms) {
        List<Resource> hits = Lists.newArrayList();

        for (String term : terms) {

            Term t = Term.parse(term);

            Statement searchQuery = select(Constants.Schema.C_TERMS_RESOURCE).from(Constants.Schema.T_TERMS)
                    .where(eq(Constants.Schema.C_TERMS_APP, Resource.DEFAULT_APPLICATION))
                    .and(  eq(Constants.Schema.C_TERMS_FIELD, t.getField()))
                    .and(  eq(Constants.Schema.C_TERMS_VALUE, t.getValue()));

            ResultSet rs = m_session.execute(searchQuery.toString()); // FIXME: toString()?

            for (Row row : rs) {
                String id = row.getString(Constants.Schema.C_TERMS_RESOURCE);
                Optional<Map<String, String>> attrs = fetchResourceAttributes(Resource.DEFAULT_APPLICATION, id);
                hits.add(new Resource(id, attrs));
            }
        }

        return hits;
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
