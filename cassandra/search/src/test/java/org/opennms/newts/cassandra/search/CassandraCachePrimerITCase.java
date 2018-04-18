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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.opennms.newts.cassandra.search.CassandraIndexerITCase.map;
import static org.opennms.newts.cassandra.search.CassandraIndexerITCase.sampleFor;

import java.util.List;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.opennms.newts.api.Resource;
import org.opennms.newts.api.Sample;
import org.opennms.newts.cassandra.CassandraSession;
import org.opennms.newts.cassandra.ContextConfigurations;
import org.opennms.newts.cassandra.NewtsInstance;

import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;

public class CassandraCachePrimerITCase {

    @Rule
    public NewtsInstance newtsInstance = new NewtsInstance();

    @Test
    public void canRestoreTheCacheContextFromCassandra() {
        CassandraSession session = newtsInstance.getCassandraSession();

        CassandraIndexingOptions options = new CassandraIndexingOptions.Builder()
                .withHierarchicalIndexing(true)
                .build();

        MetricRegistry registry = new MetricRegistry();
        GuavaResourceMetadataCache cache = new GuavaResourceMetadataCache(2048, registry);
        CassandraIndexer indexer = new CassandraIndexer(session, 0, cache, registry, options,
                new EscapableResourceIdSplitter(), new ContextConfigurations());

        Map<String, String> base = map("meat", "people", "bread", "beer");
        List<Sample> samples = Lists.newArrayList();
        samples.add(sampleFor(new Resource("a:b:c", Optional.of(base)), "m0"));
        samples.add(sampleFor(new Resource("a:b", Optional.of(base)), "m1"));
        samples.add(sampleFor(new Resource("x:b:z", Optional.of(base)), "m2"));
        indexer.update(samples);

        // Verify that the cache has some entries
        Map<String, ResourceMetadata> cacheContents = cache.getCache().asMap();
        assertThat(cacheContents.keySet(), hasSize(greaterThanOrEqualTo(3)));

        // Now prime a new cache using the data available in Cassandra
        GuavaResourceMetadataCache newCache = new GuavaResourceMetadataCache(2048, registry);
        CassandraCachePrimer cachePrimer = new CassandraCachePrimer(session);
        cachePrimer.prime(newCache);

        // We should have been able to replicate the same state
        Map<String, ResourceMetadata> primedCache = newCache.getCache().asMap();
        assertThat(cacheContents, equalTo(primedCache));
    }

}
