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
import static com.datastax.driver.core.querybuilder.QueryBuilder.bindMarker;
import static com.datastax.driver.core.querybuilder.QueryBuilder.insertInto;
import static com.datastax.driver.core.querybuilder.QueryBuilder.ttl;
import static com.datastax.driver.core.querybuilder.QueryBuilder.unloggedBatch;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.opennms.newts.api.Context;
import org.opennms.newts.api.Resource;
import org.opennms.newts.api.Sample;
import org.opennms.newts.api.search.Indexer;
import org.opennms.newts.cassandra.CassandraSession;
import org.opennms.newts.cassandra.ContextConfigurations;
import org.opennms.newts.cassandra.search.support.StatementGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.RegularStatement;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class CassandraIndexer implements Indexer {
    private static final Logger LOG = LoggerFactory.getLogger(CassandraIndexer.class);

    private final CassandraSession m_session;
    private final int m_ttl;
    private final ResourceMetadataCache m_cache;
    private final Timer m_updateTimer;
    private final Timer m_deleteTimer;
    private final Meter m_inserts;
    private final ResourceIdSplitter m_resourceIdSplitter;
    private final ContextConfigurations m_contextConfigurations;

    private final PreparedStatement m_insertTermsStatement;

    private final CassandraIndexingOptions m_options;

    private final Set<StatementGenerator> statementsInFlight = Sets.newHashSet();

    @Inject
    public CassandraIndexer(CassandraSession session, @Named("search.cassandra.time-to-live") int ttl, ResourceMetadataCache cache, MetricRegistry registry,
            CassandraIndexingOptions options, ResourceIdSplitter resourceIdSplitter, ContextConfigurations contextConfigurations) {
        m_session = checkNotNull(session, "session argument");
        m_ttl = ttl;
        m_cache = checkNotNull(cache, "cache argument");
        checkNotNull(registry, "registry argument");
        m_options = checkNotNull(options, "options argument");
        m_resourceIdSplitter = checkNotNull(resourceIdSplitter, "resourceIdSplitter argument");
        m_contextConfigurations = checkNotNull(contextConfigurations, "contextConfigurations argument");

        m_updateTimer = registry.timer(name("search", "update"));
        m_deleteTimer = registry.timer(name("search", "delete"));
        m_inserts = registry.meter(name("search", "inserts"));

        m_insertTermsStatement = session.prepare(insertInto(Constants.Schema.T_TERMS)
                .value(Constants.Schema.C_TERMS_CONTEXT, bindMarker(Constants.Schema.C_TERMS_CONTEXT))
                .value(Constants.Schema.C_TERMS_RESOURCE, bindMarker(Constants.Schema.C_TERMS_RESOURCE))
                .value(Constants.Schema.C_TERMS_FIELD, bindMarker(Constants.Schema.C_TERMS_FIELD))
                .value(Constants.Schema.C_TERMS_VALUE, bindMarker(Constants.Schema.C_TERMS_VALUE))
                .using(ttl(ttl)));
    }

    @Override
    public void update(Collection<Sample> samples) {

        Timer.Context ctx = m_updateTimer.time();

        Set<StatementGenerator> generators = Sets.newHashSet();
        Map<Context, Map<Resource, ResourceMetadata>> cacheQueue = Maps.newHashMap();

        for (Sample sample : samples) {
            maybeIndexResource(cacheQueue, generators, sample.getContext(), sample.getResource());
            maybeIndexResourceAttributes(cacheQueue, generators, sample.getContext(), sample.getResource());
            maybeAddMetricName(cacheQueue, generators, sample.getContext(), sample.getResource(), sample.getName());
        }

        try {
            if (!generators.isEmpty()) {
                synchronized(statementsInFlight) {
                    generators.removeAll(statementsInFlight);
                    statementsInFlight.addAll(generators);
                }
                m_inserts.mark(generators.size());

                // Asynchronously execute the statements
                List<ResultSetFuture> futures = Lists.newArrayList();
                for (Statement statementToExecute : toStatements(generators)) {
                    futures.add(m_session.executeAsync(statementToExecute));
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
            synchronized(statementsInFlight) {
                statementsInFlight.removeAll(generators);
            }
            ctx.stop();
        }

    }

    private List<Statement> toStatements(Set<StatementGenerator> generators) {
        List<Statement> statementsToExecute = Lists.newArrayList();

        Map<String, List<Statement>> statementsByKey = Maps.newHashMap();
        for (StatementGenerator generator : generators) {
            Statement statement = generator.toStatement()
                    .setConsistencyLevel(m_contextConfigurations.getWriteConsistency(generator.getContext()));
            String key = generator.getKey();
            if (key == null) {
                // Don't try batching these
                statementsToExecute.add(statement);
                continue;
            }

            // Group these by key
            List<Statement> statementsForKey = statementsByKey.get(key);
            if (statementsForKey == null) {
                statementsForKey = Lists.newArrayList();
                statementsByKey.put(key, statementsForKey);
            }
            statementsForKey.add(statement);
        }

        // Consolidate the grouped statements into batches
        for (List<Statement> statementsForKey: statementsByKey.values()) {
            for (List<Statement> partition : Lists.partition(statementsForKey, m_options.getMaxBatchSize())) {
                statementsToExecute.add(unloggedBatch(partition.toArray(new RegularStatement[partition.size()])));
            }
        }

        return statementsToExecute;
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

    private void recursivelyIndexResourceElements(Set<StatementGenerator> generators, Context context, String resourceId) {
        List<String> elements = m_resourceIdSplitter.splitIdIntoElements(resourceId);
        int numElements = elements.size();
        if (numElements == 1) {
            // Tag the top level elements with _parent:_root
            generators.add(new TermInsert(context, resourceId, Constants.PARENT_TERM_FIELD, Constants.TOP_LEVEL_PARENT_TERM_VALUE));
        } else {
            // Construct the parent's resource id
            String parentResourceId = m_resourceIdSplitter.joinElementsToId(elements.subList(0, numElements-1));

            // Tag the resource with its parent's id
            generators.add(new TermInsert(context, resourceId, Constants.PARENT_TERM_FIELD, parentResourceId));

            // Recurse
            recursivelyIndexResourceElements(generators, context, parentResourceId);
        }
    }

    private void maybeIndexResource(Map<Context, Map<Resource, ResourceMetadata>> cacheQueue, Set<StatementGenerator> generators, Context context, Resource resource) {
        if (!m_cache.get(context, resource).isPresent()) {
            LOG.trace("Resource '{}' in context '{}' is not present is cache.", resource, context);
            if (m_options.shouldIndexResourceTerms()) {
                for (String s : m_resourceIdSplitter.splitIdIntoElements(resource.getId())) {
                    generators.add(new TermInsert(context, resource.getId(), Constants.DEFAULT_TERM_FIELD, s));
                }
            }
            if (m_options.isHierarchicalIndexingEnabled()) {
                recursivelyIndexResourceElements(generators, context, resource.getId());
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
        if (m_options.isHierarchicalIndexingEnabled()) {
            recursivelyUnindexResourceElements(statement, context, resource.getId(), writeConsistencyLevel);
        }
    }

    private void maybeIndexResourceAttributes(Map<Context, Map<Resource, ResourceMetadata>> cacheQueue, Set<StatementGenerator> generators, Context context, Resource resource) {
        if (!resource.getAttributes().isPresent()) {
            return;
        }

        Optional<ResourceMetadata> cached = m_cache.get(context, resource);

        for (Entry<String, String> field : resource.getAttributes().get().entrySet()) {
            if (!(cached.isPresent() && cached.get().containsAttribute(field.getKey(), field.getValue()))) {
                LOG.trace("Resource attribute for resource '{}' in context '{}' for entry '{}' is not present is cache. Cached meta-data is: {}",
                        resource, context, field, cached);
                // Search indexing
                if (m_options.shouldIndexUsingDefaultTerm()) {
                    generators.add(new TermInsert(context, resource.getId(), Constants.DEFAULT_TERM_FIELD, field.getValue()));
                }
                generators.add(new TermInsert(context, resource.getId(), field.getKey(), field.getValue()));
                // Storage
                generators.add(new AttributeInsert(context, resource.getId(), field.getKey(), field.getValue()));

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

    private void maybeAddMetricName(Map<Context, Map<Resource, ResourceMetadata>> cacheQueue, Set<StatementGenerator> generators, Context context, Resource resource, String name) {
        Optional<ResourceMetadata> cached = m_cache.get(context, resource);

        if (!(cached.isPresent() && cached.get().containsMetric(name))) {
            LOG.trace("Metric resource '{}' in context '{}' with name '{}' is not present is cache. Cached meta-data is: {}",
                    resource, context, name, cached);
            generators.add(new MetricInsert(context, resource.getId(), name));

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

    private class MetricInsert implements StatementGenerator {
        private final Context m_context;
        private final String m_resourceId;
        private final String m_metric;

        public MetricInsert(Context context, String resourceId, String metric) {
            m_context = Objects.requireNonNull(context);
            m_resourceId = Objects.requireNonNull(resourceId);
            m_metric = Objects.requireNonNull(metric);
        }

        @Override
        public String getKey() {
            return String.format("(METRICS,%s,%s)", m_context.getId(), m_resourceId);
        }

        @Override
        public RegularStatement toStatement() {
            LOG.trace("Inserting metric in context: '{}' with resource id: '{}' with name: '{}'",
                    m_context, m_resourceId, m_metric);
            return insertInto(Constants.Schema.T_METRICS)
                    .value(Constants.Schema.C_METRICS_CONTEXT, m_context.getId())
                    .value(Constants.Schema.C_METRICS_RESOURCE, m_resourceId)
                    .value(Constants.Schema.C_METRICS_NAME, m_metric)
                    .using(ttl(m_ttl));
        }

        @Override
        public Context getContext() {
            return m_context;
        }

        @Override
        public int hashCode() {
            return Objects.hash(m_context, m_resourceId, m_metric);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            MetricInsert other = (MetricInsert) obj;
            return Objects.equals(this.m_context, other.m_context)
                    && Objects.equals(this.m_resourceId, other.m_resourceId)
                    && Objects.equals(this.m_metric, other.m_metric);
        }
    }

    private static abstract class KeyValuePairInsert implements StatementGenerator {
        protected final Context m_context;
        protected final String m_resourceId;
        protected final String m_field;
        protected final String m_value;

        public KeyValuePairInsert(Context context, String resourceId, String field, String value) {
            m_context = Objects.requireNonNull(context);
            m_resourceId = Objects.requireNonNull(resourceId);
            m_field = Objects.requireNonNull(field);
            m_value = Objects.requireNonNull(value);
        }

        @Override
        public Context getContext() {
            return m_context;
        }

        @Override
        public int hashCode() {
            return Objects.hash(m_context, m_resourceId, m_field, m_value);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            KeyValuePairInsert other = (KeyValuePairInsert) obj;
            return Objects.equals(this.m_context, other.m_context)
                    && Objects.equals(this.m_resourceId, other.m_resourceId)
                    && Objects.equals(this.m_field, other.m_field)
                    && Objects.equals(this.m_value, other.m_value);
        }
    }

    private class TermInsert extends KeyValuePairInsert {
        public TermInsert(Context context, String resourceId, String field, String value) {
            super(context, resourceId, field, value);
        }

        @Override
        public String getKey() {
            return null;
        }

        @Override
        public BoundStatement toStatement() {
            LOG.trace("Inserting term in context: '{}' with resource id: '{}' with field: '{}' and value: '{}'",
                    m_context, m_resourceId, m_field, m_value);
            return m_insertTermsStatement.bind()
                    .setString(Constants.Schema.C_TERMS_CONTEXT, m_context.getId())
                    .setString(Constants.Schema.C_TERMS_RESOURCE, m_resourceId)
                    .setString(Constants.Schema.C_TERMS_FIELD, m_field)
                    .setString(Constants.Schema.C_TERMS_VALUE, m_value);
        }
    }

    private class AttributeInsert extends KeyValuePairInsert {
        public AttributeInsert(Context context, String resourceId, String field, String value) {
            super(context, resourceId, field, value);
        }

        @Override
        public String getKey() {
            return String.format("(ATTRS,%s,%s)", m_context.getId(), m_resourceId);
        }

        @Override
        public RegularStatement toStatement() {
            LOG.trace("Inserting attribute in context: '{}' with resource id: '{}' with name: '{}' and value: '{}'",
                    m_context, m_resourceId, m_field, m_value);
            return insertInto(Constants.Schema.T_ATTRS)
                .value(Constants.Schema.C_ATTRS_CONTEXT, m_context.getId())
                .value(Constants.Schema.C_ATTRS_RESOURCE, m_resourceId)
                .value(Constants.Schema.C_ATTRS_ATTR, m_field)
                .value(Constants.Schema.C_ATTRS_VALUE, m_value)
                .using(ttl(m_ttl));
        }
    }
}
