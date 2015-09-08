/*
 * Copyright 2014, The OpenNMS Group
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

import static org.opennms.newts.api.search.QueryBuilder.matchKeyAndValue;
import static com.google.common.base.Preconditions.checkArgument;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.opennms.newts.api.Context;
import org.opennms.newts.api.MetricType;
import org.opennms.newts.api.Resource;
import org.opennms.newts.api.Sample;
import org.opennms.newts.api.Timestamp;
import org.opennms.newts.api.ValueType;
import org.opennms.newts.api.search.BooleanQuery;
import org.opennms.newts.api.search.Indexer;
import org.opennms.newts.api.search.Operator;
import org.opennms.newts.api.search.QueryBuilder;
import org.opennms.newts.api.search.SearchResults;
import org.opennms.newts.api.search.SearchResults.Result;
import org.opennms.newts.api.search.Term;
import org.opennms.newts.api.search.TermQuery;
import org.opennms.newts.cassandra.AbstractCassandraTestCase;
import org.opennms.newts.cassandra.CassandraSession;
import org.opennms.newts.cassandra.ContextConfigurations;
import org.opennms.newts.cassandra.search.CassandraResourceTreeWalker.SearchResultVisitor;

import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;


public class CassandraIndexerITCase extends AbstractCassandraTestCase {

    @Override
    protected String getSchemaResource() {
        return "/search_schema.cql";
    }

    @Test
    public void test() {

        Map<String, String> base = map("meat", "people", "bread", "beer");
        List<Sample> samples = Lists.newArrayList();
        samples.add(sampleFor(new Resource("aaa", Optional.of(base)), "m0"));
        samples.add(sampleFor(new Resource("aab", Optional.of(map(base, "music", "metal", "beverage", "beer"))), "m0"));
        samples.add(sampleFor(new Resource("aac:aaa", Optional.of(map(base, "music", "country"))), "m0"));

        CassandraSession session = getCassandraSession();

        ResourceMetadataCache mockCache = mock(ResourceMetadataCache.class);
        when(mockCache.get(any(Context.class), any(Resource.class))).thenReturn(Optional.<ResourceMetadata> absent());
        MetricRegistry registry = new MetricRegistry();
        ContextConfigurations contextConfigurations = new ContextConfigurations();

        Indexer indexer = new CassandraIndexer(session, 86400, mockCache, registry, false, new SimpleResourceIdSplitter(), contextConfigurations);

        indexer.update(samples);

        CassandraSearcher searcher = new CassandraSearcher(session, registry, contextConfigurations);

        // Match path components
        assertThat(searcher.search(Context.DEFAULT_CONTEXT, QueryBuilder.matchAnyValue("aaa")).size(), equalTo(2));
        assertThat(searcher.search(Context.DEFAULT_CONTEXT, QueryBuilder.matchAnyValue("aac")).size(), equalTo(1));

        // Match attribute values
        assertThat(searcher.search(Context.DEFAULT_CONTEXT, QueryBuilder.matchAnyValue("people")).size(), equalTo(3));
        assertThat(searcher.search(Context.DEFAULT_CONTEXT, QueryBuilder.matchAnyValue("metal")).size(), equalTo(1));

        // Match attribute key + value pairs
        BooleanQuery query = new BooleanQuery();
        query.add(new TermQuery(new Term("beverage", "beer")), Operator.OR);
        assertThat(searcher.search(Context.DEFAULT_CONTEXT, query).size(), equalTo(1));

        // Or'd terms
        assertThat(searcher.search(Context.DEFAULT_CONTEXT, QueryBuilder.matchAnyValue("metal", "country")).size(), equalTo(2));
        assertThat(searcher.search(Context.DEFAULT_CONTEXT, QueryBuilder.matchAnyValue("beer", "wine")).size(), equalTo(3));

        // And'd terms
        assertThat(searcher.search(Context.DEFAULT_CONTEXT, QueryBuilder.matchAllValues("metal", "country")).size(), equalTo(0));
        assertThat(searcher.search(Context.DEFAULT_CONTEXT, QueryBuilder.matchAllValues("aaa", "aac")).size(), equalTo(1));

        // Groups queries
        // (beer AND metal) OR (aaa AND country)
        BooleanQuery subquery1 = new BooleanQuery();
        subquery1.add(new TermQuery(new Term("beer")), Operator.OR);
        subquery1.add(new TermQuery(new Term("metal")), Operator.AND);

        BooleanQuery subquery2 = new BooleanQuery();
        subquery2.add(new TermQuery(new Term("aaa")), Operator.OR);
        subquery2.add(new TermQuery(new Term("country")), Operator.AND);

        query = new BooleanQuery();
        query.add(subquery1, Operator.OR);
        query.add(subquery2, Operator.OR);
        assertThat(searcher.search(Context.DEFAULT_CONTEXT, query).size(), equalTo(2));

        // Attributes are retrieved
        Result r = searcher.search(Context.DEFAULT_CONTEXT, QueryBuilder.matchAnyValue("metal")).iterator().next();
        assertThat(r.getResource().getId(), is(equalTo("aab")));
        assertThat(r.getResource().getAttributes().isPresent(), is(true));
        assertThat(r.getResource().getAttributes().get(), equalTo(map(base, "music", "metal", "beverage", "beer")));

        // Metrics too
        assertThat(r.getMetrics().size(), equalTo(1));
        assertThat(r.getMetrics().iterator().next(), equalTo("m0"));
    }

    @Test
    public void testDelete() {
        ResourceMetadataCache cache = mock(ResourceMetadataCache.class);
        when(cache.get(any(Context.class), any(Resource.class))).thenReturn(Optional.<ResourceMetadata> absent());
        MetricRegistry registry = new MetricRegistry();
        ContextConfigurations contextConfigurations = new ContextConfigurations();

        CassandraSession session = getCassandraSession();

        Indexer indexer = new CassandraIndexer(session, 86400, cache, registry, false, new SimpleResourceIdSplitter(), contextConfigurations);
        CassandraSearcher searcher = new CassandraSearcher(session, registry, contextConfigurations);

        Map<String, String> base = map("meat", "people", "bread", "beer");
        List<Sample> samples = Lists.newArrayList();
        samples.add(sampleFor(new Resource("aaa", Optional.of(base)), "m0"));
        samples.add(sampleFor(new Resource("aab", Optional.of(map(base, "music", "metal", "beverage", "beer"))), "m0"));
        samples.add(sampleFor(new Resource("aac:aaa", Optional.of(map(base, "music", "country"))), "m0"));
        indexer.update(samples);

        assertThat(searcher.search(Context.DEFAULT_CONTEXT, QueryBuilder.matchAnyValue("aaa")).size(), equalTo(2));

        indexer.delete(Context.DEFAULT_CONTEXT, new Resource("aaa", Optional.of(base)));
        assertThat(searcher.search(Context.DEFAULT_CONTEXT, QueryBuilder.matchAnyValue("aaa")).size(), equalTo(1));

        indexer.delete(Context.DEFAULT_CONTEXT, new Resource("aaa", Optional.of(base)));
        assertThat(searcher.search(Context.DEFAULT_CONTEXT, QueryBuilder.matchAnyValue("aaa")).size(), equalTo(1));

        indexer.delete(Context.DEFAULT_CONTEXT, new Resource("aac:aaa", Optional.of(base)));
        assertThat(searcher.search(Context.DEFAULT_CONTEXT, QueryBuilder.matchAnyValue("aaa")).size(), equalTo(0));
    }

    @Test
    public void canWalkTheResourceTree() {

        Map<String, String> base = map("meat", "people", "bread", "beer");
        List<Sample> samples = Lists.newArrayList();
        samples.add(sampleFor(new Resource("a:b:c", Optional.of(base)), "m0"));
        samples.add(sampleFor(new Resource("a:b", Optional.of(base)), "m1"));
        samples.add(sampleFor(new Resource("x:b:z", Optional.of(base)), "m2"));

        CassandraSession session = getCassandraSession();

        ResourceMetadataCache mockCache = mock(ResourceMetadataCache.class);
        when(mockCache.get(any(Context.class), any(Resource.class))).thenReturn(Optional.<ResourceMetadata> absent());
        MetricRegistry registry = new MetricRegistry();
        ContextConfigurations contextConfigurations = new ContextConfigurations();

        Indexer indexer = new CassandraIndexer(session, 86400, mockCache, registry, true, new SimpleResourceIdSplitter(), contextConfigurations);

        indexer.update(samples);

        CassandraSearcher searcher = new CassandraSearcher(session, registry, contextConfigurations);

        // Verify specific search results
        SearchResults results = searcher.search(Context.DEFAULT_CONTEXT, matchKeyAndValue("_parent", "_root"));
        Iterator<Result> it = results.iterator();
        Result result = it.next();
        assertThat(result.getResource().getId(), equalTo("a"));
        // a is a resource with no metrics
        assertThat(result.getMetrics().size(), equalTo(0));
        result = it.next();
        assertThat(result.getResource().getId(), equalTo("x"));
        // x is a resource with no metrics
        assertThat(result.getMetrics().size(), equalTo(0));

        results = searcher.search(Context.DEFAULT_CONTEXT, matchKeyAndValue("_parent", "a"));
        result = results.iterator().next();
        assertThat(result.getResource().getId(), equalTo("a:b"));
        assertThat(result.getMetrics().size(), equalTo(1));

        results = searcher.search(Context.DEFAULT_CONTEXT, matchKeyAndValue("_parent", "a:b"));
        result = results.iterator().next();
        assertThat(result.getResource().getId(), equalTo("a:b:c"));
        assertThat(result.getMetrics().size(), equalTo(1));

        results = searcher.search(Context.DEFAULT_CONTEXT, matchKeyAndValue("_parent", "a:b:c"));
        assertThat(results.iterator().hasNext(), equalTo(false));

        // Walk the tree via BFS
        LoggingResourceVisitor visitor = new LoggingResourceVisitor();
        CassandraResourceTreeWalker resourceTreeWalker = new CassandraResourceTreeWalker(searcher);
        resourceTreeWalker.breadthFirstSearch(Context.DEFAULT_CONTEXT, visitor);
        assertThat(visitor.getResourceIds(), equalTo(Lists.newArrayList(
                "a", "x", "a:b", "x:b", "a:b:c", "x:b:z")));

        // Walk the tree via DFS
        visitor = new LoggingResourceVisitor();
        resourceTreeWalker.depthFirstSearch(Context.DEFAULT_CONTEXT, visitor);
        assertThat(visitor.getResourceIds(), equalTo(Lists.newArrayList(
                "a", "a:b", "a:b:c", "x", "x:b", "x:b:z")));
    }

    private static class LoggingResourceVisitor implements SearchResultVisitor {
        private List<Result> results = Lists.newArrayList();

        @Override
        public boolean visit(Result result) {
            return results.add(result);
        }

        public ArrayList<String> getResourceIds() {
            ArrayList<String> resourceIds = Lists.newArrayList();
            for (Result result : results) {
                resourceIds.add(result.getResource().getId());
            }
            return resourceIds;
        }
    };

    @Test
    public void testCache() {

        ResourceMetadataCache cache = mock(ResourceMetadataCache.class);
        when(cache.get(any(Context.class), any(Resource.class))).thenReturn(Optional.<ResourceMetadata> absent());
        MetricRegistry registry = new MetricRegistry();
        ContextConfigurations contextConfigurations = new ContextConfigurations();

        Indexer indexer = new CassandraIndexer(getCassandraSession(), 86400, cache, registry, false, new SimpleResourceIdSplitter(), contextConfigurations);

        Sample s = sampleFor(new Resource("aaa", Optional.of(map("beverage", "beer"))), "m0");
        indexer.update(Collections.singletonList(s));

        ResourceMetadata expected = new ResourceMetadata().putMetric("m0").putAttribute("beverage", "beer");

        verify(cache, atLeast(1)).get(any(Context.class), any(Resource.class));
        verify(cache).merge(any(Context.class), any(Resource.class), eq(expected));

    }

    private CassandraSession getCassandraSession() {
        return new CassandraSession(CASSANDRA_KEYSPACE, CASSANDRA_HOST, CASSANDRA_PORT, CASSANDRA_COMPRESSION);
    }

    /** Creates a sample (any sample), for a given resource and metric name. */
    private Sample sampleFor(Resource resource, String metric) {
        return new Sample(Timestamp.now(), resource, metric, MetricType.GAUGE, ValueType.compose(0.0d, MetricType.GAUGE));
    }

    /** Returns a Map from an even number of strings **/
    private Map<String, String> map(String... attrs) {
        return map(Maps.<String, String> newHashMap(), attrs);
    }

    /** Returns a Map from an even number of strings, and a (copy of a )base map */
    private Map<String, String> map(Map<String, String> base, String... attrs) {
        checkArgument((attrs.length % 2) == 0, "odd number of attrs!");

        Map<String, String> map = Maps.newHashMap(base);

        for (int i = 0; i < attrs.length; i += 2) {
            map.put(attrs[i], attrs[i + 1]);
        }

        return map;
    }

}
