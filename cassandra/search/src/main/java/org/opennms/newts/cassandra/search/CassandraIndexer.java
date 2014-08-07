package org.opennms.newts.cassandra.search;


import static com.datastax.driver.core.querybuilder.QueryBuilder.batch;
import static com.datastax.driver.core.querybuilder.QueryBuilder.insertInto;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;

import javax.inject.Inject;

import org.opennms.newts.api.Context;
import org.opennms.newts.api.Resource;
import org.opennms.newts.api.Sample;
import org.opennms.newts.api.search.Indexer;
import org.opennms.newts.cassandra.CassandraSession;

import com.datastax.driver.core.RegularStatement;
import com.google.common.collect.Lists;


public class CassandraIndexer implements Indexer {

    private CassandraSession m_session;

    @Inject
    public CassandraIndexer(CassandraSession session) {
        m_session = checkNotNull(session, "session argument");
    }

    @Override
    public void update(Collection<Sample> samples) {

        List<RegularStatement> statements = Lists.newArrayList();

        for (Sample sample : samples) {
            maybeIndexResourceAttributes(statements, sample.getContext(), sample.getResource());
            maybeAddResourceAttributes(statements, sample.getContext(), sample.getResource());
            maybeAddMetricName(statements, sample.getContext(), sample.getResource(), sample.getName());
        }

        if (statements.size() > 0) {
            m_session.execute(batch(statements.toArray(new RegularStatement[0])).toString());   // FIXME: toString()?
        }

    }

    // TODO: Make these inserts conditional on presence in a cache of seen attributes
    private void maybeIndexResourceAttributes(List<RegularStatement> statement, Context context, Resource resource) {
        if (!resource.getAttributes().isPresent()) {
            return;
        }

        for (Entry<String, String> field : resource.getAttributes().get().entrySet()) {
            statement.add(
                    insertInto(Constants.Schema.T_TERMS)
                        .value(Constants.Schema.C_TERMS_CONTEXT, context.getId())
                        .value(Constants.Schema.C_TERMS_FIELD, Constants.DEFAULT_TERM_FIELD)
                        .value(Constants.Schema.C_TERMS_VALUE, field.getValue())
                        .value(Constants.Schema.C_TERMS_RESOURCE, resource.getId())
            );
            statement.add(
                    insertInto(Constants.Schema.T_TERMS)
                        .value(Constants.Schema.C_TERMS_CONTEXT, context.getId())
                        .value(Constants.Schema.C_TERMS_FIELD, field.getKey())
                        .value(Constants.Schema.C_TERMS_VALUE, field.getValue())
                        .value(Constants.Schema.C_TERMS_RESOURCE, resource.getId())
            );
        }
    }

    // TODO: Make these inserts conditional on presence in a cache of seen attributes
    private void maybeAddResourceAttributes(List<RegularStatement> statement, Context context, Resource resource) {
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
    private void maybeAddMetricName(List<RegularStatement> statement, Context context, Resource resource, String name) {
        statement.add(
                insertInto(Constants.Schema.T_METRICS)
                    .value(Constants.Schema.C_METRICS_CONTEXT, context.getId())
                    .value(Constants.Schema.C_METRICS_RESOURCE, resource.getId())
                    .value(Constants.Schema.C_METRICS_NAME, name)
        );
    }

}
