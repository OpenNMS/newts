/*
 * Copyright 2016, The OpenNMS Group
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
import static com.datastax.driver.core.querybuilder.QueryBuilder.batch;
import static com.datastax.driver.core.querybuilder.QueryBuilder.insertInto;
import static com.datastax.driver.core.querybuilder.QueryBuilder.ttl;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.inject.Inject;
import javax.inject.Named;

import com.datastax.driver.core.querybuilder.QueryBuilder;
import org.opennms.newts.api.Context;
import org.opennms.newts.api.Resource;
import org.opennms.newts.api.Sample;
import org.opennms.newts.api.search.Indexer;
import org.opennms.newts.cassandra.CassandraSession;
import org.opennms.newts.cassandra.ContextConfigurations;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.RegularStatement;
import com.datastax.driver.core.ResultSetFuture;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;


public class CassandraIndexer implements Indexer {

    private static final int MAX_BATCH_SIZE = 16;

    private final CassandraSession m_session;
    private final int m_ttl;
    private final ResourceMetadataCache m_cache;
    private final Timer m_updateTimer;
    private final Timer m_deleteTimer;
    private final boolean m_isHierarchicalIndexingEnabled;
    private final ResourceIdSplitter m_resourceIdSplitter;
    private final ContextConfigurations m_contextConfigurations;

    @Inject
    public CassandraIndexer(CassandraSession session, @Named("search.cassandra.time-to-live") int ttl, ResourceMetadataCache cache, MetricRegistry registry,
            @Named("search.hierarical-indexing") boolean isHierarchicalIndexingEnabled,
            ResourceIdSplitter resourceIdSplitter, ContextConfigurations contextConfigurations) {
        m_session = checkNotNull(session, "session argument");
        m_ttl = ttl;
        m_cache = checkNotNull(cache, "cache argument");
        checkNotNull(registry, "registry argument");
        m_isHierarchicalIndexingEnabled = isHierarchicalIndexingEnabled;
        m_resourceIdSplitter = checkNotNull(resourceIdSplitter, "resourceIdSplitter argument");
        m_contextConfigurations = checkNotNull(contextConfigurations, "contextConfigurations argument");

        m_updateTimer = registry.timer(name("search", "update"));
        m_deleteTimer = registry.timer(name("search", "delete"));

    }

    @Override
    public void update(Collection<Sample> samples) {

        Timer.Context ctx = m_updateTimer.time();

        List<RegularStatement> statements = Lists.newArrayList();
        Map<Context, Map<Resource, ResourceMetadata>> cacheQueue = Maps.newHashMap();

        for (Sample sample : samples) {
            ConsistencyLevel writeConsistency = m_contextConfigurations.getWriteConsistency(sample.getContext());
            maybeIndexResource(cacheQueue, statements, sample.getContext(), sample.getResource(), writeConsistency);
            maybeIndexResourceAttributes(cacheQueue, statements, sample.getContext(), sample.getResource(), writeConsistency);
            maybeAddMetricName(cacheQueue, statements, sample.getContext(), sample.getResource(), sample.getName(), writeConsistency);
        }

        try {
            if (!statements.isEmpty()) {
                // Deduplicate the insert statements by keying off the effective query strings
                TreeMap<String, RegularStatement> cqlToStatementMap = new TreeMap<String, RegularStatement>();
                for (RegularStatement statement : statements) {
                    cqlToStatementMap.put(statement.toString(), statement);
                }
                statements = Lists.newArrayList(cqlToStatementMap.values());

                // Limit the size of the batches; See NEWTS-67
                List<ResultSetFuture> futures = Lists.newArrayList();
                for (List<RegularStatement> partition : Lists.partition(statements, MAX_BATCH_SIZE)) {
                    futures.add(m_session.executeAsync(batch(partition.toArray(new RegularStatement[partition.size()]))));
                }

                for (ResultSetFuture future : futures) {
                    future.getUninterruptibly();
                }
            }

            // Order matters here; We want the cache updated only after a successful Cassandra write.
            for (Context context : cacheQueue.keySet()) {
                for (Map.Entry<Resource, ResourceMetadata> entry : cacheQueue.get(context).entrySet()) {
                    m_cache.merge(context, entry.getKey(), entry.getValue());
                }
            }
        }
        finally {
            ctx.stop();
        }

    }

    @Override
    public void delete(final Context context, final Resource resource) {
        final Timer.Context ctx = m_deleteTimer.time();

        final ConsistencyLevel writeConsistency = m_contextConfigurations.getWriteConsistency(context);

        final List<RegularStatement> statements = Lists.newArrayList();
        definitelyUnindexResource(statements, context, resource, writeConsistency);
        definitelyUnindexResourceAttributes(statements, context, resource, writeConsistency);
        definitelyRemoveMetricName(statements, context, resource, writeConsistency);

        try {
            if (!statements.isEmpty()) {
                m_session.execute(batch(statements.toArray(new RegularStatement[statements.size()])));
            }

            m_cache.delete(context, resource);
        } finally {
            ctx.stop();
        }
    }

    private void recursivelyIndexResourceElements(List<RegularStatement> statement, Context context, String resourceId, ConsistencyLevel writeConsistencyLevel) {
        List<String> elements = m_resourceIdSplitter.splitIdIntoElements(resourceId);
        int numElements = elements.size();
        if (numElements == 1) {
            // Tag the top level elements with _parent:_root
            RegularStatement insert = insertInto(Constants.Schema.T_TERMS)
                .value(Constants.Schema.C_TERMS_CONTEXT, context.getId())
                .value(Constants.Schema.C_TERMS_FIELD, Constants.PARENT_TERM_FIELD)
                .value(Constants.Schema.C_TERMS_VALUE, Constants.TOP_LEVEL_PARENT_TERM_VALUE)
                .value(Constants.Schema.C_TERMS_RESOURCE, resourceId)
                .using(ttl(m_ttl));
            insert.setConsistencyLevel(writeConsistencyLevel);
            statement.add(insert);
        } else {
            // Construct the parent's resource id
            String parentResourceId = m_resourceIdSplitter.joinElementsToId(elements.subList(0, numElements-1));

            // Tag the resource with its parent's id
            RegularStatement insert = insertInto(Constants.Schema.T_TERMS)
                .value(Constants.Schema.C_TERMS_CONTEXT, context.getId())
                .value(Constants.Schema.C_TERMS_FIELD, Constants.PARENT_TERM_FIELD)
                .value(Constants.Schema.C_TERMS_VALUE, parentResourceId)
                .value(Constants.Schema.C_TERMS_RESOURCE, resourceId)
                .using(ttl(m_ttl));
            insert.setConsistencyLevel(writeConsistencyLevel);
            statement.add(insert);

            // Recurse
            recursivelyIndexResourceElements(statement, context, parentResourceId, writeConsistencyLevel);
        }
    }

    private void maybeIndexResource(Map<Context, Map<Resource, ResourceMetadata>> cacheQueue, List<RegularStatement> statement, Context context, Resource resource, ConsistencyLevel writeConsistencyLevel) {
        if (!m_cache.get(context, resource).isPresent()) {
            for (String s : m_resourceIdSplitter.splitIdIntoElements(resource.getId())) {
                RegularStatement insert = insertInto(Constants.Schema.T_TERMS)
                    .value(Constants.Schema.C_TERMS_CONTEXT, context.getId())
                    .value(Constants.Schema.C_TERMS_FIELD, Constants.DEFAULT_TERM_FIELD)
                    .value(Constants.Schema.C_TERMS_VALUE, s)
                    .value(Constants.Schema.C_TERMS_RESOURCE, resource.getId())
                    .using(ttl(m_ttl));
                insert.setConsistencyLevel(writeConsistencyLevel);
                statement.add(insert);
            }
            if (m_isHierarchicalIndexingEnabled) {
                recursivelyIndexResourceElements(statement, context, resource.getId(), writeConsistencyLevel);
            }

            getOrCreateResourceMetadata(context, resource, cacheQueue);
        }
    }

    private void recursivelyUnindexResourceElements(List<RegularStatement> statement, Context context, String resourceId, ConsistencyLevel writeConsistencyLevel) {
        List<String> elements = m_resourceIdSplitter.splitIdIntoElements(resourceId);
        int numElements = elements.size();
        if (numElements == 1) {
            // Tag the top level elements with _parent:_root
            RegularStatement delete = QueryBuilder.delete()
                    .from(Constants.Schema.T_TERMS)
                    .where(QueryBuilder.eq(Constants.Schema.C_TERMS_CONTEXT, context.getId()))
                    .and(QueryBuilder.eq(Constants.Schema.C_TERMS_FIELD, Constants.PARENT_TERM_FIELD))
                    .and(QueryBuilder.eq(Constants.Schema.C_TERMS_VALUE, Constants.TOP_LEVEL_PARENT_TERM_VALUE))
                    .and(QueryBuilder.eq(Constants.Schema.C_TERMS_RESOURCE, resourceId));
            delete.setConsistencyLevel(writeConsistencyLevel);
            statement.add(delete);
        } else {
            // Construct the parent's resource id
            String parentResourceId = m_resourceIdSplitter.joinElementsToId(elements.subList(0, numElements-1));

            // Tag the resource with its parent's id
            RegularStatement delete = QueryBuilder.delete()
                    .from(Constants.Schema.T_TERMS)
                    .where(QueryBuilder.eq(Constants.Schema.C_TERMS_CONTEXT, context.getId()))
                    .and(QueryBuilder.eq(Constants.Schema.C_TERMS_FIELD, Constants.PARENT_TERM_FIELD))
                    .and(QueryBuilder.eq(Constants.Schema.C_TERMS_VALUE, parentResourceId))
                    .and(QueryBuilder.eq(Constants.Schema.C_TERMS_RESOURCE, resourceId));
            delete.setConsistencyLevel(writeConsistencyLevel);
            statement.add(delete);

            // Recurse
            recursivelyUnindexResourceElements(statement, context, parentResourceId, writeConsistencyLevel);
        }
    }

    private void definitelyUnindexResource(List<RegularStatement> statement, Context context, Resource resource, ConsistencyLevel writeConsistencyLevel) {
        for (String s : m_resourceIdSplitter.splitIdIntoElements(resource.getId())) {
            RegularStatement delete = QueryBuilder.delete()
                .from(Constants.Schema.T_TERMS)
                .where(QueryBuilder.eq(Constants.Schema.C_TERMS_CONTEXT, context.getId()))
                .and(QueryBuilder.eq(Constants.Schema.C_TERMS_FIELD, Constants.DEFAULT_TERM_FIELD))
                .and(QueryBuilder.eq(Constants.Schema.C_TERMS_VALUE, s))
                .and(QueryBuilder.eq(Constants.Schema.C_TERMS_RESOURCE, resource.getId()));
            delete.setConsistencyLevel(writeConsistencyLevel);
            statement.add(delete);
        }
        if (m_isHierarchicalIndexingEnabled) {
            recursivelyUnindexResourceElements(statement, context, resource.getId(), writeConsistencyLevel);
        }
    }

    private void maybeIndexResourceAttributes(Map<Context, Map<Resource, ResourceMetadata>> cacheQueue, List<RegularStatement> statement, Context context, Resource resource, ConsistencyLevel writeConsistency) {
        if (!resource.getAttributes().isPresent()) {
            return;
        }

        Optional<ResourceMetadata> cached = m_cache.get(context, resource);

        for (Entry<String, String> field : resource.getAttributes().get().entrySet()) {
            if (!(cached.isPresent() && cached.get().containsAttribute(field.getKey(), field.getValue()))) {
                // Search indexing
                RegularStatement insert = insertInto(Constants.Schema.T_TERMS)
                    .value(Constants.Schema.C_TERMS_CONTEXT, context.getId())
                    .value(Constants.Schema.C_TERMS_FIELD, Constants.DEFAULT_TERM_FIELD)
                    .value(Constants.Schema.C_TERMS_VALUE, field.getValue())
                    .value(Constants.Schema.C_TERMS_RESOURCE, resource.getId())
                    .using(ttl(m_ttl));
                insert.setConsistencyLevel(writeConsistency);
                statement.add(insert);
                insert = insertInto(Constants.Schema.T_TERMS)
                    .value(Constants.Schema.C_TERMS_CONTEXT, context.getId())
                    .value(Constants.Schema.C_TERMS_FIELD, field.getKey())
                    .value(Constants.Schema.C_TERMS_VALUE, field.getValue())
                    .value(Constants.Schema.C_TERMS_RESOURCE, resource.getId())
                    .using(ttl(m_ttl));
                insert.setConsistencyLevel(writeConsistency);
                statement.add(insert);
                // Storage
                insert = insertInto(Constants.Schema.T_ATTRS)
                    .value(Constants.Schema.C_ATTRS_CONTEXT, context.getId())
                    .value(Constants.Schema.C_ATTRS_RESOURCE, resource.getId())
                    .value(Constants.Schema.C_ATTRS_ATTR, field.getKey())
                    .value(Constants.Schema.C_ATTRS_VALUE, field.getValue())
                    .using(ttl(m_ttl));
                insert.setConsistencyLevel(writeConsistency);
                statement.add(insert);

                getOrCreateResourceMetadata(context, resource, cacheQueue).putAttribute(field.getKey(), field.getValue());
            }
        }
    }

    private void definitelyUnindexResourceAttributes(List<RegularStatement> statement, Context context, Resource resource, ConsistencyLevel writeConsistency) {
        if (!resource.getAttributes().isPresent()) {
            return;
        }

        for (Entry<String, String> field : resource.getAttributes().get().entrySet()) {
            // Search unindexing
            RegularStatement delete = QueryBuilder.delete().from(Constants.Schema.T_TERMS)
                    .where(QueryBuilder.eq(Constants.Schema.C_TERMS_CONTEXT, context.getId()))
                    .and(QueryBuilder.eq(Constants.Schema.C_TERMS_FIELD, Constants.DEFAULT_TERM_FIELD))
                    .and(QueryBuilder.eq(Constants.Schema.C_TERMS_VALUE, field.getValue()))
                    .and(QueryBuilder.eq(Constants.Schema.C_TERMS_RESOURCE, resource.getId()));
            delete.setConsistencyLevel(writeConsistency);
            statement.add(delete);
            delete = QueryBuilder.delete().from(Constants.Schema.T_TERMS)
                    .where(QueryBuilder.eq(Constants.Schema.C_TERMS_CONTEXT, context.getId()))
                    .and(QueryBuilder.eq(Constants.Schema.C_TERMS_FIELD, field.getKey()))
                    .and(QueryBuilder.eq(Constants.Schema.C_TERMS_VALUE, field.getValue()))
                    .and(QueryBuilder.eq(Constants.Schema.C_TERMS_RESOURCE, resource.getId()));
            delete.setConsistencyLevel(writeConsistency);
            statement.add(delete);
            // Storage
            delete = QueryBuilder.delete().from(Constants.Schema.T_ATTRS)
                    .where(QueryBuilder.eq(Constants.Schema.C_ATTRS_CONTEXT, context.getId()))
                    .and(QueryBuilder.eq(Constants.Schema.C_ATTRS_RESOURCE, resource.getId()))
                    .and(QueryBuilder.eq(Constants.Schema.C_ATTRS_ATTR, field.getKey()));
            delete.setConsistencyLevel(writeConsistency);
            statement.add(delete);
        }
    }

    private void maybeAddMetricName(Map<Context, Map<Resource, ResourceMetadata>> cacheQueue, List<RegularStatement> statement, Context context, Resource resource, String name, ConsistencyLevel writeConsistency) {

        Optional<ResourceMetadata> cached = m_cache.get(context, resource);

        if (!(cached.isPresent() && cached.get().containsMetric(name))) {
            RegularStatement insert = insertInto(Constants.Schema.T_METRICS)
                .value(Constants.Schema.C_METRICS_CONTEXT, context.getId())
                .value(Constants.Schema.C_METRICS_RESOURCE, resource.getId())
                .value(Constants.Schema.C_METRICS_NAME, name)
                .using(ttl(m_ttl));
            insert.setConsistencyLevel(writeConsistency);
            statement.add(insert);

            getOrCreateResourceMetadata(context, resource, cacheQueue).putMetric(name);
        }
    }

    private void definitelyRemoveMetricName(List<RegularStatement> statement, Context context, Resource resource, ConsistencyLevel writeConsistency) {
        RegularStatement delete = QueryBuilder.delete().from(Constants.Schema.T_METRICS)
                .where(QueryBuilder.eq(Constants.Schema.C_METRICS_CONTEXT, context.getId()))
                .and(QueryBuilder.eq(Constants.Schema.C_METRICS_RESOURCE, resource.getId()));
        delete.setConsistencyLevel(writeConsistency);
        statement.add(delete);
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
