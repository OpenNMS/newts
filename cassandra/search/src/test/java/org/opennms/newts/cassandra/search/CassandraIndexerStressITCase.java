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

import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.opennms.newts.api.Counter;
import org.opennms.newts.api.MetricType;
import org.opennms.newts.api.Resource;
import org.opennms.newts.api.Sample;
import org.opennms.newts.api.Timestamp;
import org.opennms.newts.cassandra.CassandraSession;
import org.opennms.newts.cassandra.ContextConfigurations;
import org.opennms.newts.cassandra.NewtsInstance;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.Lists;

public class CassandraIndexerStressITCase {

    @Rule
    public NewtsInstance newtsInstance = new NewtsInstance();

    @Test
    public void canIndexManyResources() {
        final int numResources = 20000;
        final int numSamplesPerResource = 3;

        CassandraSession session = newtsInstance.getCassandraSession();

        ContextConfigurations contexts = new ContextConfigurations();
        MetricRegistry metrics = new MetricRegistry();

        CassandraIndexingOptions options = new CassandraIndexingOptions.Builder()
                .withHierarchicalIndexing(true).build();

        ResourceIdSplitter resourceIdSplitter = new EscapableResourceIdSplitter();
        GuavaResourceMetadataCache cache = new GuavaResourceMetadataCache(numResources * 2, metrics);
        CassandraIndexer indexer = new CassandraIndexer(session, 0, cache, metrics, options,
                resourceIdSplitter, contexts);

        // Generate the resources and sample sets
        Resource resources[] = new Resource[numResources];
        List<List<Sample>> sampleSets = Lists.newArrayListWithCapacity(numResources);
        System.out.println("Building sample sets...");
        for (int i = 0; i < numResources; i++) {
            resources[i] = new Resource(String.format("snmp:%d:eth0-x:ifHcInOctets", i));
            List<Sample> samples = Lists.newArrayListWithCapacity(numSamplesPerResource);
            for (int j = 0; j < numSamplesPerResource; j++) {
                samples.add(new Sample(Timestamp.now(), resources[i], "y" + j, MetricType.COUNTER, new Counter(i * j)));
            }
            sampleSets.add(samples);
        };
        System.out.println("Done building sample sets.");

        // Index the resources and associated samples several times over
        for (int k = 0; k < 3; k++) {
            System.out.println("Indexing samples sets...");
            long start = System.currentTimeMillis();
            for (List<Sample> sampleSet : sampleSets) {
                indexer.update(sampleSet);
            }
            long elapsed = System.currentTimeMillis() - start;
            System.out.println("Done indexing samples in : " + elapsed + " ms");
        }
    }
}
