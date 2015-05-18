package org.opennms.newts.cassandra.search;


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

import java.util.Collections;
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
import org.opennms.newts.api.search.SearchResults.Result;
import org.opennms.newts.api.search.Term;
import org.opennms.newts.api.search.TermQuery;
import org.opennms.newts.cassandra.AbstractCassandraTestCase;
import org.opennms.newts.cassandra.CassandraSession;

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

        Indexer indexer = new CassandraIndexer(session, 86400, mockCache, registry);

        indexer.update(samples);

        CassandraSearcher searcher = new CassandraSearcher(session, registry);

        // Match path components
        assertThat(searcher.search(QueryBuilder.matchAnyValue("aaa")).size(), equalTo(2));
        assertThat(searcher.search(QueryBuilder.matchAnyValue("aac")).size(), equalTo(1));

        // Match attribute values
        assertThat(searcher.search(QueryBuilder.matchAnyValue("people")).size(), equalTo(3));
        assertThat(searcher.search(QueryBuilder.matchAnyValue("metal")).size(), equalTo(1));

        // Match attribute key + value pairs
        BooleanQuery query = new BooleanQuery();
        query.add(new TermQuery(new Term("beverage", "beer")), Operator.OR);
        assertThat(searcher.search(query).size(), equalTo(1));

        // Or'd terms
        assertThat(searcher.search(QueryBuilder.matchAnyValue("metal", "country")).size(), equalTo(2));
        assertThat(searcher.search(QueryBuilder.matchAnyValue("beer", "wine")).size(), equalTo(3));

        // And'd terms
        assertThat(searcher.search(QueryBuilder.matchAllValues("metal", "country")).size(), equalTo(0));
        assertThat(searcher.search(QueryBuilder.matchAllValues("aaa", "aac")).size(), equalTo(1));

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
        assertThat(searcher.search(query).size(), equalTo(2));

        // Attributes are retrieved
        Result r = searcher.search(QueryBuilder.matchAnyValue("metal")).iterator().next();
        assertThat(r.getResource().getId(), is(equalTo("aab")));
        assertThat(r.getResource().getAttributes().isPresent(), is(true));
        assertThat(r.getResource().getAttributes().get(), equalTo(map(base, "music", "metal", "beverage", "beer")));

        // Metrics too
        assertThat(r.getMetrics().size(), equalTo(1));
        assertThat(r.getMetrics().iterator().next(), equalTo("m0"));
    }

    @Test
    public void testCache() {

        ResourceMetadataCache cache = mock(ResourceMetadataCache.class);
        when(cache.get(any(Context.class), any(Resource.class))).thenReturn(Optional.<ResourceMetadata> absent());
        MetricRegistry registry = new MetricRegistry();

        Indexer indexer = new CassandraIndexer(getCassandraSession(), 86400, cache, registry);

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
