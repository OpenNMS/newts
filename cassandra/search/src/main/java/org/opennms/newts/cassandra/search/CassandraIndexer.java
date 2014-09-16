package org.opennms.newts.cassandra.search;


import static com.datastax.driver.core.querybuilder.QueryBuilder.batch;
import static com.datastax.driver.core.querybuilder.QueryBuilder.insertInto;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;

import org.opennms.newts.api.Context;
import org.opennms.newts.api.Resource;
import org.opennms.newts.api.Sample;
import org.opennms.newts.api.search.Indexer;
import org.opennms.newts.cassandra.CassandraSession;

import com.datastax.driver.core.RegularStatement;
import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;


public class CassandraIndexer implements Indexer {

    private static Splitter s_pathSplitter = Splitter.on('/').omitEmptyStrings().trimResults();

    private CassandraSession m_session;
    private ResourceMetadataCache m_cache;

    @Inject
    public CassandraIndexer(CassandraSession session, ResourceMetadataCache cache) {
        m_session = checkNotNull(session, "session argument");
        m_cache = checkNotNull(cache, "cache argument");
    }

    @Override
    public void update(Collection<Sample> samples) {

        List<RegularStatement> statements = Lists.newArrayList();
        Map<Context, Map<Resource, ResourceMetadata>> cacheQueue = Maps.newHashMap();

        // TODO: Deduplicate resources & metrics to minimize size of batch insert.
        for (Sample sample : samples) {
            maybeIndexResource(cacheQueue, statements, sample.getContext(), sample.getResource());
            maybeIndexResourceAttributes(cacheQueue, statements, sample.getContext(), sample.getResource());
            maybeAddMetricName(cacheQueue, statements, sample.getContext(), sample.getResource(), sample.getName());
        }

        if (statements.size() > 0) {
            m_session.execute(batch(statements.toArray(new RegularStatement[0])).toString());   // FIXME: toString()?
        }

        // Order matters here; We want the cache updated only after a successful Cassandra write.
        for (Context context : cacheQueue.keySet()) {
            for (Map.Entry<Resource, ResourceMetadata> entry : cacheQueue.get(context).entrySet()) {
                m_cache.merge(context, entry.getKey(), entry.getValue());
            }
        }

    }

    private void maybeIndexResource(Map<Context, Map<Resource, ResourceMetadata>> cacheQueue, List<RegularStatement> statement, Context context, Resource resource) {
        if (!m_cache.get(context, resource).isPresent()) {
            for (String s : s_pathSplitter.split(resource.getId())) {
                statement.add(
                        insertInto(Constants.Schema.T_TERMS)
                            .value(Constants.Schema.C_TERMS_CONTEXT, context.getId())
                            .value(Constants.Schema.C_TERMS_FIELD, Constants.DEFAULT_TERM_FIELD)
                            .value(Constants.Schema.C_TERMS_VALUE, s)
                            .value(Constants.Schema.C_TERMS_RESOURCE, resource.getId())
                );
            }

            getOrCreateResourceMetadata(context, resource, cacheQueue);
        }
    }

    private void maybeIndexResourceAttributes(Map<Context, Map<Resource, ResourceMetadata>> cacheQueue, List<RegularStatement> statement, Context context, Resource resource) {
        if (!resource.getAttributes().isPresent()) {
            return;
        }

        Optional<ResourceMetadata> cached = m_cache.get(context, resource);
        
        for (Entry<String, String> field : resource.getAttributes().get().entrySet()) {
            if (!(cached.isPresent() && cached.get().containsAttribute(field.getKey(), field.getValue()))) {
                // Search indexing
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
                // Storage
                statement.add(
                        insertInto(Constants.Schema.T_ATTRS)
                            .value(Constants.Schema.C_ATTRS_CONTEXT, context.getId())
                            .value(Constants.Schema.C_ATTRS_RESOURCE, resource.getId())
                            .value(Constants.Schema.C_ATTRS_ATTR, field.getKey())
                            .value(Constants.Schema.C_ATTRS_VALUE, field.getValue())
                );

                getOrCreateResourceMetadata(context, resource, cacheQueue).putAttribute(field.getKey(), field.getValue());
            }
        }
    }

    private void maybeAddMetricName(Map<Context, Map<Resource, ResourceMetadata>> cacheQueue, List<RegularStatement> statement, Context context, Resource resource, String name) {

        Optional<ResourceMetadata> cached = m_cache.get(context, resource);

        if (!(cached.isPresent() && cached.get().containsMetric(name))) {
            statement.add(
                    insertInto(Constants.Schema.T_METRICS)
                        .value(Constants.Schema.C_METRICS_CONTEXT, context.getId())
                        .value(Constants.Schema.C_METRICS_RESOURCE, resource.getId())
                        .value(Constants.Schema.C_METRICS_NAME, name)
            );
            
            getOrCreateResourceMetadata(context, resource, cacheQueue).putMetric(name);
        }
    }

    private static ResourceMetadata getOrCreateResourceMetadata(Context context, Resource resource, Map<Context, Map<Resource, ResourceMetadata>> map) {

        Map<Resource, ResourceMetadata> inner = map.get(context);
        if (inner == null) {
            inner = Maps.newHashMap();
            map.put(context, inner);
        }

        ResourceMetadata rMeta = inner.get(resource);
        if (rMeta == null) {
            rMeta = new ResourceMetadata();
            inner.put(resource, rMeta);
        }

        return rMeta;
    }

}
