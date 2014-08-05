package org.opennms.newts.cassandra.search;


import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;

import java.util.Collection;
import java.util.List;

import javax.inject.Inject;

import org.opennms.newts.api.Resource;
import org.opennms.newts.cassandra.CassandraSession;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Statement;
import com.google.common.collect.Lists;


public class CassandraSearcher {

    private CassandraSession m_session;

    @Inject
    public CassandraSearcher(CassandraSession session) {
        m_session = session; // FIXME: non-null session
    }

    public Collection<Resource> search(String term) {
        return search("_all", term);
    }

    public Collection<Resource> search(String field, String term) {
        List<Resource> hits = Lists.newArrayList();

        Statement searchQuery = select(Constants.Schema.C_TERMS_RESOURCE).from(Constants.Schema.T_TERMS)
                .where(eq(Constants.Schema.C_TERMS_APP, Resource.DEFAULT_APPLICATION))
                .and(  eq(Constants.Schema.C_TERMS_FIELD, field))
                .and(  eq(Constants.Schema.C_TERMS_TERM, term));

        ResultSet rs = m_session.execute(searchQuery.toString()); // FIXME: toString()?

        for (Row row : rs) {
            hits.add(new Resource(row.getString(Constants.Schema.C_TERMS_RESOURCE)));
        }

        return hits;
    }

}
