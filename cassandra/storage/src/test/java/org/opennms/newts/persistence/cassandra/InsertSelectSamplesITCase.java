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
package org.opennms.newts.persistence.cassandra;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.opennms.newts.api.MetricType.GAUGE;

import java.util.Iterator;
import java.util.List;

import org.junit.Test;
import org.opennms.newts.api.Context;
import org.opennms.newts.api.Duration;
import org.opennms.newts.api.Gauge;
import org.opennms.newts.api.Resource;
import org.opennms.newts.api.Sample;
import org.opennms.newts.api.Results.Row;
import org.opennms.newts.api.Timestamp;

import com.datastax.driver.core.ConsistencyLevel;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;


public class InsertSelectSamplesITCase extends AbstractCassandraTestCase {

    @Test
    public void test() {

        List<Sample> samples = Lists.newArrayList();
        int rows = 10, cols = 3;
        Resource resource = new Resource("r");

        for (int i = 1; i <= rows; i++) {
            Timestamp ts = Timestamp.fromEpochMillis(i * 1000);

            for (int j = 1; j <= cols; j++) {
                samples.add(new Sample(ts, resource, "m" + j, GAUGE, new Gauge((i + 1) * j)));
            }
        }

        // Override the shard period to ensure we test query concurrency
        m_contextConfigurations.addContextConfig(Context.DEFAULT_CONTEXT,
                Duration.seconds(1), ConsistencyLevel.ALL, ConsistencyLevel.ALL);

        getRepository().insert(samples);

        Timestamp start = Timestamp.fromEpochMillis(0), end = Timestamp.fromEpochMillis(rows * 1000);
        Iterator<Row<Sample>> results = getRepository().select(Context.DEFAULT_CONTEXT, resource,
                Optional.of(start), Optional.of(end)).iterator();

        for (int i = 1; i <= rows; i++) {
            assertTrue("Insufficient number of results", results.hasNext());

            Timestamp timestamp = Timestamp.fromEpochMillis(i * 1000);
            Row<Sample> row = results.next();

            assertEquals("Unexpected timestamp for row " + i, timestamp, row.getTimestamp());
            assertEquals("Unexpected resource name", resource, row.getResource());

            for (int j = 1; j <= cols; j++) {
                assertNotNull("Missing sample: m" + j, row.getElement("m" + j));

                Sample sample = row.getElement("m" + j);

                assertEquals("Unexpected timestamp for metric m" + j, timestamp, sample.getTimestamp());
                assertEquals("Unexpected resource name", resource, sample.getResource());
                assertEquals("Unexpected metric name", "m" + j, sample.getName());
                assertEquals("Unexpected metric type", GAUGE, sample.getType());
                assertEquals((double) ((i + 1) * j), sample.getValue().doubleValue(), 0.0d);
            }

        }

    }

}
