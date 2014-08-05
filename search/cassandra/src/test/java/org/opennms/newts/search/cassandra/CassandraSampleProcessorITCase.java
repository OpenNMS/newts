package org.opennms.newts.search.cassandra;


import static com.google.common.base.Preconditions.checkArgument;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.lucene.store.Directory;
import org.junit.Test;
import org.opennms.newts.api.MetricType;
import org.opennms.newts.api.Resource;
import org.opennms.newts.api.Sample;
import org.opennms.newts.api.Timestamp;
import org.opennms.newts.api.ValueType;
import org.opennms.newts.api.search.Indexer;
import org.opennms.newts.api.search.QueryParseException;
import org.opennms.newts.api.search.Searcher;
import org.opennms.newts.cassandra.AbstractCassandraTestCase;
import org.opennms.newts.search.cassandra.lucene.CassandraDirectory;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;


public class CassandraSampleProcessorITCase extends AbstractCassandraTestCase {

    @Test
    public void test() throws IOException, QueryParseException {

        // Index
        try (Directory directory = getDirectory()) {
            Indexer indexer = new CassandraLuceneIndexer(directory);
            Map<String, String> base = attributesFor("fruit", "apple", "meat", "people");

            List<Sample> samples = Lists.newArrayList();
            samples.add(sampleFor(new Resource("/aaa", Optional.of(base))));
            samples.add(sampleFor(new Resource("/aab", Optional.of(attributesFor(base, "music", "metal")))));
            samples.add(sampleFor(new Resource("/aac", Optional.of(attributesFor(base, "music", "country")))));

            indexer.update(samples);
            ((CassandraLuceneIndexer) indexer).close(); // FIXME: No.
        }

        // Search
        try (Directory directory = getDirectory()) {
            Searcher searcher = new CassandraLuceneSearcher(directory);
            Collection<Resource> hits = searcher.search("apple");

            assertThat(hits.size(), equalTo(3));

            hits = searcher.search("country");

            assertThat(hits.size(), equalTo(1));
            assertThat(hits.iterator().next().getId(), is("/aac"));

            hits = searcher.search("music:country");

            assertThat(hits.size(), equalTo(1));
            assertThat(hits.iterator().next().getId(), is("/aac"));

            // XXX: Why does this have to be a prefix query to work?
            hits = searcher.search("\\/aab*");

            assertThat(hits.size(), equalTo(1));
            Resource r = hits.iterator().next();
            assertThat(r.getAttributes().isPresent(), is(true));
            assertThat(r.getAttributes().get().containsKey("music"), is(true));
            assertThat(r.getAttributes().get().get("music"), is(equalTo("metal")));

        }

    }

    /** Creates a sample (any sample), for a given resource. */
    private Sample sampleFor(Resource resource) {
        return new Sample(Timestamp.now(), resource, "m0", MetricType.GAUGE, ValueType.compose(0.0d, MetricType.GAUGE));
    }

    /** Returns a Cassandra-connected Lucene Directory implementation */
    private Directory getDirectory() throws IOException {
        return new CassandraDirectory(CASSANDRA_KEYSPACE, CASSANDRA_HOST, CASSANDRA_PORT, getClass().getSimpleName());
    }

    /** Returns a Map from an even number of strings **/
    private Map<String, String> attributesFor(String... attrs) {
        return attributesFor(Maps.<String, String> newHashMap(), attrs);
    }

    /** Returns a Map from an even number of strings, and a (copy of a )base map */
    private Map<String, String> attributesFor(Map<String, String> base, String... attrs) {
        checkArgument((attrs.length % 2) == 0, "odd number of attrs!");

        Map<String, String> map = Maps.newHashMap(base);

        for (int i = 0; i < attrs.length; i += 2) {
            map.put(attrs[i], attrs[i + 1]);
        }

        return map;
    }

}
