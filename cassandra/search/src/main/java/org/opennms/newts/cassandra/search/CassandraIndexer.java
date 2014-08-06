package org.opennms.newts.cassandra.search;


import static com.datastax.driver.core.querybuilder.QueryBuilder.batch;
import static com.datastax.driver.core.querybuilder.QueryBuilder.insertInto;

import java.util.Collection;
import java.util.Map.Entry;

import javax.inject.Inject;

import org.opennms.newts.api.Sample;
import org.opennms.newts.api.search.Indexer;
import org.opennms.newts.cassandra.CassandraSession;

import com.datastax.driver.core.querybuilder.Batch;


public class CassandraIndexer implements Indexer {

    private CassandraSession m_session;

    @Inject
    public CassandraIndexer(CassandraSession session) {
        m_session = session; // FIXME: non-null session
    }

    @Override
    public void update(Collection<Sample> samples) {

        Batch batch = batch();

        for (Sample sample : samples) {
            for (Entry<String, String> field : sample.getResource().getAttributes().get().entrySet()) {
                batch.add(
                        insertInto(Constants.Schema.T_TERMS)
                            .value(Constants.Schema.C_TERMS_APP, sample.getResource().getApplication())
                            .value(Constants.Schema.C_TERMS_FIELD, "_all")  // FIXME: don't hard-code
                            .value(Constants.Schema.C_TERMS_TERM, field.getValue())
                            .value(Constants.Schema.C_TERMS_RESOURCE, sample.getResource().getId())
                );
                batch.add(
                        insertInto(Constants.Schema.T_TERMS)
                            .value(Constants.Schema.C_TERMS_APP, sample.getResource().getApplication())
                            .value(Constants.Schema.C_TERMS_FIELD, field.getKey())
                            .value(Constants.Schema.C_TERMS_TERM, field.getValue())
                            .value(Constants.Schema.C_TERMS_RESOURCE, sample.getResource().getId())
                );
            }
        }

        m_session.execute(batch.toString());

    }

}
