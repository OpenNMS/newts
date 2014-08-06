package org.opennms.newts.cassandra.search;


import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.List;

import javax.inject.Inject;

import org.opennms.newts.api.Resource;
import org.opennms.newts.cassandra.CassandraSession;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Statement;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;


public class CassandraSearcher {

    private CassandraSession m_session;

    @Inject
    public CassandraSearcher(CassandraSession session) {
        m_session = checkNotNull(session, "session argument");
    }

    public Collection<Resource> search(String... terms) {
        List<Resource> hits = Lists.newArrayList();

        for (String term : terms) {

            Term t = Term.parse(term);

            Statement searchQuery = select(Constants.Schema.C_TERMS_RESOURCE).from(Constants.Schema.T_TERMS)
                    .where(eq(Constants.Schema.C_TERMS_APP, Resource.DEFAULT_APPLICATION))
                    .and(  eq(Constants.Schema.C_TERMS_FIELD, t.getField()))
                    .and(  eq(Constants.Schema.C_TERMS_TERM, t.getValue()));

            ResultSet rs = m_session.execute(searchQuery.toString()); // FIXME: toString()?

            for (Row row : rs) {
                hits.add(new Resource(row.getString(Constants.Schema.C_TERMS_RESOURCE)));
            }
        }

        return hits;
    }

    static class Term {

        static String DEFAULT_FIELD = "_all";

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

            String field = t.size() < 2 ? DEFAULT_FIELD : t.get(0);
            String value = t.size() < 2 ? t.get(0) : t.get(1);

            return new Term(field, value);
        }

    }

}
