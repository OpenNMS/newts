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


import static org.opennms.newts.persistence.cassandra.Utils.assertAttributes;
import static org.opennms.newts.persistence.cassandra.Utils.assertRowsEqual;
import static org.opennms.newts.persistence.cassandra.Utils.mapFor;
import static org.opennms.newts.api.query.StandardAggregationFunctions.AVERAGE;

import java.util.Iterator;
import java.util.List;

import org.junit.Test;
import org.opennms.newts.persistence.cassandra.Utils.MeasurementRowsBuilder;
import org.opennms.newts.persistence.cassandra.Utils.SampleRowsBuilder;
import org.opennms.newts.api.Duration;
import org.opennms.newts.api.Measurement;
import org.opennms.newts.api.MetricType;
import org.opennms.newts.api.Resource;
import org.opennms.newts.api.Results;
import org.opennms.newts.api.Results.Row;
import org.opennms.newts.api.Sample;
import org.opennms.newts.api.Timestamp;
import org.opennms.newts.api.query.ResultDescriptor;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;

public class InsertSelectMeasurementsITCase extends AbstractCassandraTestCase {

    private void writeSamples(Iterator<Row<Sample>> samples) {
        List<Sample> writeSamples = Lists.newArrayList();

        while (samples.hasNext()) {
            writeSamples.addAll(samples.next().getElements());
        }

        // Override the shard period to ensure we test query concurrency
        getRepository().setResourceShard(Duration.seconds(600));

        getRepository().insert(writeSamples);

    }

    @Test
    public void test() {

        Iterator<Row<Sample>> testSamples = new SampleRowsBuilder(new Resource("localhost"), MetricType.GAUGE)
                .row(900000000).element("m0", 1)        // Thu Jul  9 11:00:00 CDT 1998
                .row(900000300).element("m0", 1)
                .row(900000600).element("m0", 1)
                .row(900000900).element("m0", 1)
                .row(900001200).element("m0", 1)
                .row(900001500).element("m0", 1)
                .row(900001800).element("m0", 1, mapFor("a", "1"))
                .row(900002100).element("m0", 3)
                .row(900002400).element("m0", 3, mapFor("b", "2"))
                .row(900002700).element("m0", 3)
                .row(900003000).element("m0", 3)
                .row(900003300).element("m0", 3)
                .row(900003600).element("m0", 3)
                .row(900003900).element("m0", 1)
                .row(900004200).element("m0", 1)
                .row(900004500).element("m0", 1)
                .row(900004800).element("m0", 1)
                .row(900005100).element("m0", 1, mapFor("c", "3"))
                .row(900005400).element("m0", 1)
                .row(900005700).element("m0", 3, mapFor("d", "4"))
                .row(900006000).element("m0", 3)
                .row(900006300).element("m0", 3)
                .row(900006600).element("m0", 3)
                .row(900006900).element("m0", 3)
                .row(900007200).element("m0", 3)        // Thu Jul  9 13:00:00 CDT 1998
                .build();

        ResultDescriptor rDescriptor = new ResultDescriptor(Duration.seconds(300))
            .datasource("m0-avg", "m0", Duration.seconds(600), AVERAGE).export("m0-avg");

        Iterator<Row<Measurement>> expected = new MeasurementRowsBuilder(new Resource("localhost"))
                .row(900003600).element("m0-avg", 2)
                .row(900007200).element("m0-avg", 2)
                .build();

        writeSamples(testSamples);

        Results<Measurement> results = getRepository().select(
                new Resource("localhost"),
                Optional.of(Timestamp.fromEpochSeconds(900003600)),
                Optional.of(Timestamp.fromEpochSeconds(900007200)),
                rDescriptor,
                Duration.minutes(60));

        // Validate results
        assertRowsEqual(expected, results.iterator());

        // Validate merged attributes
        Iterator<Row<Measurement>> rows = results.iterator();
        assertAttributes(rows.next().getElement("m0-avg"), mapFor("a", "1", "b", "2"));
        assertAttributes(rows.next().getElement("m0-avg"), mapFor("c", "3", "d", "4"));

    }

}
