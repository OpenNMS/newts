package org.opennms.newts.cassandra.search;


import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.opennms.newts.api.MetricType;
import org.opennms.newts.api.Resource;
import org.opennms.newts.api.Sample;
import org.opennms.newts.api.Timestamp;
import org.opennms.newts.api.ValueType;
import org.opennms.newts.api.search.Indexer;
import org.opennms.newts.cassandra.AbstractCassandraTestCase;
import org.opennms.newts.cassandra.CassandraSession;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;


public class CassandraIndexerITCase extends AbstractCassandraTestCase {

    @Test
    public void test() {

        Map<String, String> base = map("meat", "people", "bread", "beer");
        List<Sample> samples = Lists.newArrayList();

        samples.add(sampleFor(new Resource("/aaa", Optional.of(base))));
        samples.add(sampleFor(new Resource("/aab", Optional.of(map(base, "music", "metal", "beverage", "beer")))));
        samples.add(sampleFor(new Resource("/aac", Optional.of(map(base, "music", "country")))));

        CassandraSession session = getCassandraSession();
        Indexer indexer = new CassandraIndexer(session);

        indexer.update(samples);

        CassandraSearcher searcher = new CassandraSearcher(session);

        assertThat(searcher.search("people").size(), equalTo(3));
        assertThat(searcher.search("metal").size(), equalTo(1));
        assertThat(searcher.search("beverage:beer").size(), equalTo(1));

        // Or'd terms
        assertThat(searcher.search("metal", "country").size(), equalTo(2));
        assertThat(searcher.search("beer", "wine").size(), equalTo(3));

    }

    private CassandraSession getCassandraSession() {
        return new CassandraSession(CASSANDRA_KEYSPACE, CASSANDRA_HOST, CASSANDRA_PORT);
    }

    /** Creates a sample (any sample), for a given resource. */
    private Sample sampleFor(Resource resource) {
        return new Sample(Timestamp.now(), resource, "m0", MetricType.GAUGE, ValueType.compose(0.0d, MetricType.GAUGE));
    }

    /** Returns a Map from an even number of strings **/
    private Map<String, String> map(String... attrs) {
        return map(Maps.<String, String> newHashMap(), attrs);
    }

    /** Returns a Map from an even number of strings, and a (copy of a )base map */
    private Map<String, String> map(Map<String, String> base, String... attrs) {
        Preconditions.checkArgument((attrs.length % 2) == 0, "odd number of attrs!");

        Map<String, String> map = Maps.newHashMap(base);

        for (int i = 0; i < attrs.length; i += 2) {
            map.put(attrs[i], attrs[i + 1]);
        }

        return map;
    }

}
