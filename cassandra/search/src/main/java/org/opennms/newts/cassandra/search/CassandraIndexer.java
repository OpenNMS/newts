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
import static com.datastax.oss.driver.api.querybuilder.QueryBuilder.bindMarker;
import static com.datastax.oss.driver.api.querybuilder.QueryBuilder.insertInto;
import static com.datastax.oss.driver.api.querybuilder.QueryBuilder.literal;
import static com.datastax.oss.driver.api.querybuilder.select.Selector.ttl;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

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
import com.datastax.oss.driver.api.core.ConsistencyLevel;
import com.datastax.oss.driver.api.core.cql.AsyncResultSet;
import com.datastax.oss.driver.api.core.cql.BatchStatement;
import com.datastax.oss.driver.api.core.cql.BatchStatementBuilder;
import com.datastax.oss.driver.api.core.cql.BatchableStatement;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.DefaultBatchType;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.datastax.oss.driver.api.core.cql.Statement;
import com.datastax.oss.driver.api.querybuilder.QueryBuilder;
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
    public CassandraIndexer(CassandraSession session, @Named("search.cassandra.time-to-live") int ttl, ResourceMetadataCache cache, @Named("newtsMetricRegistry") MetricRegistry registry,
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
                .usingTtl(ttl).build());
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
                List<CompletionStage<AsyncResultSet>> futures = Lists.newArrayList();
                for (Statement<?> statementToExecute : toStatements(generators)) {
                    futures.add(m_session.executeAsync(statementToExecute));
                }

                for (CompletionStage<AsyncResultSet> future : futures) {
                    try {
                        future.toCompletableFuture().get();
                    } catch (InterruptedException|ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            // Order matters here; We want the cache updated only after a successful Cassandra write.
            for (Context context : cacheQueue.keySet()) {
                for (Map.Entry<Resource, ResourceMetadata> entry : cacheQueue.get(context).entrySet()) {
                    m_cache.merge(context, entry.getKey(), entry.getValue());
                }
            }
        } finally {
            synchronized(statementsInFlight) {
                statementsInFlight.removeAll(generators);
            }
            ctx.stop();
        }
    }

    private List<Statement> toStatements(Set<StatementGenerator> generators) {
        List<Statement> statementsToExecute = Lists.newArrayList();

        Map<String, List<BatchableStatement>> statementsByKey = Maps.newHashMap();
        for (StatementGenerator generator : generators) {
            BatchableStatement statement = generator.toStatement()
                    .setConsistencyLevel(m_contextConfigurations.getWriteConsistency(generator.getContext()));
            String key = generator.getKey();
            if (key == null) {
                // Don't try batching these
                statementsToExecute.add(statement);
                continue;
            }

            // Group these by key
            List<BatchableStatement> statementsForKey = statementsByKey.get(key);
            if (statementsForKey == null) {
                statementsForKey = Lists.newArrayList();
                statementsByKey.put(key, statementsForKey);
            }
            statementsForKey.add(statement);
        }

        // Consolidate the grouped statements into batches
        for (List<BatchableStatement> statementsForKey: statementsByKey.values()) {
            for (List<BatchableStatement> partition : Lists.partition(statementsForKey, m_options.getMaxBatchSize())) {
                BatchStatementBuilder builder = BatchStatement.builder(DefaultBatchType.UNLOGGED);
                for (BatchableStatement statement : partition) {
                    builder.addStatement(statement);
                }
                statementsToExecute.add(builder.build());
            }
        }

        return statementsToExecute;
    }

    @Override
    public void delete(final Context context, final Resource resource) {
        final Timer.Context ctx = m_deleteTimer.time();

        final ConsistencyLevel writeConsistency = m_contextConfigurations.getWriteConsistency(context);

        final List<SimpleStatement> statements = Lists.newArrayList();
        definitelyUnindexResource(statements, context, resource, writeConsistency);
        definitelyUnindexResourceAttributes(statements, context, resource, writeConsistency);
        definitelyRemoveMetricName(statements, context, resource, writeConsistency);

        try {
            if (!statements.isEmpty()) {
                BatchStatementBuilder builder = BatchStatement.builder(DefaultBatchType.LOGGED);
                for (SimpleStatement statement : statements) {
                    builder.addStatement(statement);
                }
                m_session.execute(builder.build());
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
        Optional<ResourceMetadata> cached = m_cache.get(context, resource);

        if (!(cached.isPresent() && !cached.get().expired(System.currentTimeMillis()))) {
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

    private void recursivelyUnindexResourceElements(List<SimpleStatement> statement, Context context, String resourceId, ConsistencyLevel writeConsistencyLevel) {
        List<String> elements = m_resourceIdSplitter.splitIdIntoElements(resourceId);
        int numElements = elements.size();
        if (numElements == 1) {
            // Tag the top level elements with _parent:_root
            SimpleStatement delete = QueryBuilder.deleteFrom(Constants.Schema.T_TERMS)
                    .whereColumn(Constants.Schema.C_TERMS_CONTEXT).isEqualTo(literal(context.getId()))
                    .whereColumn(Constants.Schema.C_TERMS_FIELD).isEqualTo(literal(Constants.PARENT_TERM_FIELD))
                    .whereColumn(Constants.Schema.C_TERMS_VALUE).isEqualTo(literal(Constants.TOP_LEVEL_PARENT_TERM_VALUE))
                    .whereColumn(Constants.Schema.C_TERMS_RESOURCE).isEqualTo(literal(resourceId))
                    .build()
                    .setConsistencyLevel(writeConsistencyLevel);
            statement.add(delete);
        } else {
            // Construct the parent's resource id
            String parentResourceId = m_resourceIdSplitter.joinElementsToId(elements.subList(0, numElements-1));

            // Tag the resource with its parent's id
            SimpleStatement delete = QueryBuilder.deleteFrom(Constants.Schema.T_TERMS)
                    .whereColumn(Constants.Schema.C_TERMS_CONTEXT).isEqualTo(literal(context.getId()))
                    .whereColumn(Constants.Schema.C_TERMS_FIELD).isEqualTo(literal(Constants.PARENT_TERM_FIELD))
                    .whereColumn(Constants.Schema.C_TERMS_VALUE).isEqualTo(literal(parentResourceId))
                    .whereColumn(Constants.Schema.C_TERMS_RESOURCE).isEqualTo(literal(resourceId))
                    .build()
                    .setConsistencyLevel(writeConsistencyLevel);
            statement.add(delete);

            // Recurse
            recursivelyUnindexResourceElements(statement, context, parentResourceId, writeConsistencyLevel);
        }
    }

    private void definitelyUnindexResource(List<SimpleStatement> statement, Context context, Resource resource, ConsistencyLevel writeConsistencyLevel) {
        for (String s : m_resourceIdSplitter.splitIdIntoElements(resource.getId())) {
            SimpleStatement delete = QueryBuilder.deleteFrom(Constants.Schema.T_TERMS)
                    .whereColumn(Constants.Schema.C_TERMS_CONTEXT).isEqualTo(literal(context.getId()))
                    .whereColumn(Constants.Schema.C_TERMS_FIELD).isEqualTo(literal(Constants.DEFAULT_TERM_FIELD))
                    .whereColumn(Constants.Schema.C_TERMS_VALUE).isEqualTo(literal(s))
                    .whereColumn(Constants.Schema.C_TERMS_RESOURCE).isEqualTo(literal(resource.getId()))
                    .build()
                    .setConsistencyLevel(writeConsistencyLevel);
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
            if (!(cached.isPresent() && !cached.get().expired(System.currentTimeMillis()) && cached.get().containsAttribute(field.getKey(), field.getValue()))) {
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

    private void definitelyUnindexResourceAttributes(List<SimpleStatement> statement, Context context, Resource resource, ConsistencyLevel writeConsistency) {
        if (!resource.getAttributes().isPresent()) {
            return;
        }

        for (Entry<String, String> field : resource.getAttributes().get().entrySet()) {
            // Search unindexing
            SimpleStatement delete = QueryBuilder.deleteFrom(Constants.Schema.T_TERMS)
                    .whereColumn(Constants.Schema.C_TERMS_CONTEXT).isEqualTo(literal(context.getId()))
                    .whereColumn(Constants.Schema.C_TERMS_FIELD).isEqualTo(literal(Constants.DEFAULT_TERM_FIELD))
                    .whereColumn(Constants.Schema.C_TERMS_VALUE).isEqualTo(literal(field.getValue()))
                    .whereColumn(Constants.Schema.C_TERMS_RESOURCE).isEqualTo(literal(resource.getId()))
                    .build()
                    .setConsistencyLevel(writeConsistency);
            statement.add(delete);
            delete = QueryBuilder.deleteFrom(Constants.Schema.T_TERMS)
                    .whereColumn(Constants.Schema.C_TERMS_CONTEXT).isEqualTo(literal(context.getId()))
                    .whereColumn(Constants.Schema.C_TERMS_FIELD).isEqualTo(literal(field.getKey()))
                    .whereColumn(Constants.Schema.C_TERMS_VALUE).isEqualTo(literal(field.getValue()))
                    .whereColumn(Constants.Schema.C_TERMS_RESOURCE).isEqualTo(literal(resource.getId()))
                    .build()
                    .setConsistencyLevel(writeConsistency);
            statement.add(delete);
            // Storage
            delete = QueryBuilder.deleteFrom(Constants.Schema.T_ATTRS)
                    .whereColumn(Constants.Schema.C_ATTRS_CONTEXT).isEqualTo(literal(context.getId()))
                    .whereColumn(Constants.Schema.C_ATTRS_RESOURCE).isEqualTo(literal(resource.getId()))
                    .whereColumn(Constants.Schema.C_ATTRS_ATTR).isEqualTo(literal(field.getKey()))
                    .build()
                    .setConsistencyLevel(writeConsistency);
            statement.add(delete);
        }
    }

    private void maybeAddMetricName(Map<Context, Map<Resource, ResourceMetadata>> cacheQueue, Set<StatementGenerator> generators, Context context, Resource resource, String name) {
        Optional<ResourceMetadata> cached = m_cache.get(context, resource);

        if (!(cached.isPresent() && !cached.get().expired(System.currentTimeMillis()) && cached.get().containsMetric(name))) {
            LOG.trace("Metric resource '{}' in context '{}' with name '{}' is not present is cache. Cached meta-data is: {}",
                    resource, context, name, cached);
            generators.add(new MetricInsert(context, resource.getId(), name));

            getOrCreateResourceMetadata(context, resource, cacheQueue).putMetric(name);
        }
    }

    private void definitelyRemoveMetricName(List<SimpleStatement> statement, Context context, Resource resource, ConsistencyLevel writeConsistency) {
        SimpleStatement delete = QueryBuilder.deleteFrom(Constants.Schema.T_METRICS)
                .whereColumn(Constants.Schema.C_METRICS_CONTEXT).isEqualTo(literal(context.getId()))
                .whereColumn(Constants.Schema.C_METRICS_RESOURCE).isEqualTo(literal(resource.getId()))
                .build()
                .setConsistencyLevel(writeConsistency);
        statement.add(delete);
    }

    private ResourceMetadata getOrCreateResourceMetadata(Context context, Resource resource, Map<Context, Map<Resource, ResourceMetadata>> map) {

        Map<Resource, ResourceMetadata> inner = map.get(context);
        if (inner == null) {
            inner = Maps.newHashMap();
            map.put(context, inner);
        }

        ResourceMetadata rMeta = inner.get(resource);
        if (rMeta == null) {
            // Let the caches expire before the real TTL to avoid corner-cases and add some margin
            final long expires = System.currentTimeMillis() + this.m_ttl * 1000L * 3L / 4L;

            rMeta = new ResourceMetadata().setExpires(expires);
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
        public SimpleStatement toStatement() {
            LOG.trace("Inserting metric in context: '{}' with resource id: '{}' with name: '{}'",
                    m_context, m_resourceId, m_metric);
            return insertInto(Constants.Schema.T_METRICS)
                    .value(Constants.Schema.C_METRICS_CONTEXT, literal(m_context.getId()))
                    .value(Constants.Schema.C_METRICS_RESOURCE, literal(m_resourceId))
                    .value(Constants.Schema.C_METRICS_NAME, literal(m_metric))
                    .usingTtl(m_ttl)
                    .build();
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
            return m_insertTermsStatement.boundStatementBuilder()
                    .setString(Constants.Schema.C_TERMS_CONTEXT, m_context.getId())
                    .setString(Constants.Schema.C_TERMS_RESOURCE, m_resourceId)
                    .setString(Constants.Schema.C_TERMS_FIELD, m_field)
                    .setString(Constants.Schema.C_TERMS_VALUE, m_value)
                    .build();
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
        public SimpleStatement toStatement() {
            LOG.trace("Inserting attribute in context: '{}' with resource id: '{}' with name: '{}' and value: '{}'",
                    m_context, m_resourceId, m_field, m_value);
            return insertInto(Constants.Schema.T_ATTRS)
                .value(Constants.Schema.C_ATTRS_CONTEXT, literal(m_context.getId()))
                .value(Constants.Schema.C_ATTRS_RESOURCE, literal(m_resourceId))
                .value(Constants.Schema.C_ATTRS_ATTR, literal(m_field))
                .value(Constants.Schema.C_ATTRS_VALUE, literal(m_value))
                .usingTtl(m_ttl)
                .build();
        }
    }
}
