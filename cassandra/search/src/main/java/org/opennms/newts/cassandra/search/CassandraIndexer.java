package org.opennms.newts.cassandra.search;


import static com.datastax.driver.core.querybuilder.QueryBuilder.batch;
import static com.datastax.driver.core.querybuilder.QueryBuilder.insertInto;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.Map.Entry;

import javax.inject.Inject;

import org.opennms.newts.api.Context;
import org.opennms.newts.api.Resource;
import org.opennms.newts.api.Sample;
import org.opennms.newts.api.search.Indexer;
import org.opennms.newts.cassandra.CassandraSession;

import com.datastax.driver.core.querybuilder.Batch;


public class CassandraIndexer implements Indexer {

    private CassandraSession m_session;

    @Inject
    public CassandraIndexer(CassandraSession session) {
        m_session = checkNotNull(session, "session argument");
    }

    @Override
    public void update(Collection<Sample> samples) {

        Batch batch = batch();

        for (Sample sample : samples) {
            for (Entry<String, String> field : sample.getResource().getAttributes().get().entrySet()) {
                batch.add(
                        insertInto(Constants.Schema.T_TERMS)
                            .value(Constants.Schema.C_TERMS_CONTEXT, sample.getContext().getId())
                            .value(Constants.Schema.C_TERMS_FIELD, Constants.DEFAULT_TERM_FIELD)
                            .value(Constants.Schema.C_TERMS_VALUE, field.getValue())
                            .value(Constants.Schema.C_TERMS_RESOURCE, sample.getResource().getId())
                );
                batch.add(
                        insertInto(Constants.Schema.T_TERMS)
                            .value(Constants.Schema.C_TERMS_CONTEXT, sample.getContext().getId())
                            .value(Constants.Schema.C_TERMS_FIELD, field.getKey())
                            .value(Constants.Schema.C_TERMS_VALUE, field.getValue())
                            .value(Constants.Schema.C_TERMS_RESOURCE, sample.getResource().getId())
                );
            }

            maybeAddResourceAttributes(batch, sample.getContext(), sample.getResource());
            maybeAddMetricName(batch, sample.getContext(), sample.getResource(), sample.getName());

        }

        m_session.execute(batch.toString());

    }

    // TODO: Make the add conditional on metric's presence in a cache of "seen" metrics.
    private void maybeAddResourceAttributes(Batch statement, Context context, Resource resource) {
        if (!resource.getAttributes().isPresent()) {
            return;
        }

        for (Entry<String, String> attr : resource.getAttributes().get().entrySet()) {
            statement.add(
                    insertInto(Constants.Schema.T_ATTRS)
                        .value(Constants.Schema.C_ATTRS_CONTEXT, context.getId())
                        .value(Constants.Schema.C_ATTRS_RESOURCE, resource.getId())
                        .value(Constants.Schema.C_ATTRS_ATTR, attr.getKey())
                        .value(Constants.Schema.C_ATTRS_VALUE, attr.getValue())
            );
        }
    }

    // TODO: Make the add conditional on metric's presence in a cache of "seen" metrics.
    private void maybeAddMetricName(Batch statement, Context context, Resource resource, String name) {
        statement.add(
                insertInto(Constants.Schema.T_METRICS)
                    .value(Constants.Schema.C_METRICS_CONTEXT, context.getId())
                    .value(Constants.Schema.C_METRICS_RESOURCE, resource.getId())
                    .value(Constants.Schema.C_METRICS_NAME, name)
        );
    }

}
