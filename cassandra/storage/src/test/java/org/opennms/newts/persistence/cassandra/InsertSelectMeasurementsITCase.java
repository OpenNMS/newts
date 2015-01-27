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
                .row(900000000).element("mGauge", 1)        // Thu Jul  9 11:00:00 CDT 1998
                .row(900000300).element("mGauge", 1)
                .row(900000600).element("mGauge", 1)
                .row(900000900).element("mGauge", 1)
                .row(900001200).element("mGauge", 1)
                .row(900001500).element("mGauge", 1)
                .row(900001800).element("mGauge", 1, mapFor("a", "1"))
                .row(900002100).element("mGauge", 3)
                .row(900002400).element("mGauge", 3, mapFor("b", "2"))
                .row(900002700).element("mGauge", 3)
                .row(900003000).element("mGauge", 3)
                .row(900003300).element("mGauge", 3)
                .row(900003600).element("mGauge", 3)
                .row(900003900).element("mGauge", 1)
                .row(900004200).element("mGauge", 1)
                .row(900004500).element("mGauge", 1)
                .row(900004800).element("mGauge", 1)
                .row(900005100).element("mGauge", 1, mapFor("c", "3"))
                .row(900005400).element("mGauge", 1)
                .row(900005700).element("mGauge", 3, mapFor("d", "4"))
                .row(900006000).element("mGauge", 3)
                .row(900006300).element("mGauge", 3)
                .row(900006600).element("mGauge", 3)
                .row(900006900).element("mGauge", 3)
                .row(900007200).element("mGauge", 3)        // Thu Jul  9 13:00:00 CDT 1998
                .build();

        ResultDescriptor rDescriptor = new ResultDescriptor(Duration.seconds(300))
            .datasource("mGauge-avg", "mGauge", Duration.seconds(600), AVERAGE).export("mGauge-avg");

        Iterator<Row<Measurement>> expected = new MeasurementRowsBuilder(new Resource("localhost"))
                .row(900003600).element("mGauge-avg", 2)
                .row(900007200).element("mGauge-avg", 2)
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
        assertAttributes(rows.next().getElement("mGauge-avg"), mapFor("a", "1", "b", "2"));
        assertAttributes(rows.next().getElement("mGauge-avg"), mapFor("c", "3", "d", "4"));

    }

    @Test
    public void testWithCounter() {

        Iterator<Row<Sample>> testSamples = new SampleRowsBuilder(new Resource("localhost"), MetricType.COUNTER)
                .row(900000000).element("mCounter",    0)        // Thu Jul  9 11:00:00 CDT 1998
                .row(900000300).element("mCounter",  300)
                .row(900000600).element("mCounter",  600)
                .row(900000900).element("mCounter",  900)
                .row(900001200).element("mCounter", 1200)
                .row(900001500).element("mCounter", 1500)
                .row(900001800).element("mCounter", 1800, mapFor("a", "1"))
                .row(900002100).element("mCounter", 2100)
                .row(900002400).element("mCounter", 2400, mapFor("b", "2"))
                .row(900002700).element("mCounter", 2700)
                .row(900003000).element("mCounter", 3000)
                .row(900003300).element("mCounter", 3300)
                .row(900003600).element("mCounter", 3600)
                .row(900003900).element("mCounter", 3900)
                .row(900004200).element("mCounter", 4200)
                .row(900004500).element("mCounter", 4500)
                .row(900004800).element("mCounter", 4800)
                .row(900005100).element("mCounter", 5100, mapFor("c", "3"))
                .row(900005400).element("mCounter", 5400)
                .row(900005700).element("mCounter", 5700, mapFor("d", "4"))
                .row(900006000).element("mCounter", 6000)
                .row(900006300).element("mCounter", 6300)
                .row(900006600).element("mCounter", 6600)
                .row(900006900).element("mCounter", 6900)
                .row(900007200).element("mCounter", 7200)        // Thu Jul  9 13:00:00 CDT 1998
                .build();

        ResultDescriptor rDescriptor = new ResultDescriptor(Duration.seconds(300))
            .datasource("mCounter-avg", "mCounter", Duration.seconds(600), AVERAGE).export("mCounter-avg");

        Iterator<Row<Measurement>> expected = new MeasurementRowsBuilder(new Resource("localhost"))
                .row(900003600).element("mCounter-avg", 1)
                .row(900007200).element("mCounter-avg", 1)
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
        assertAttributes(rows.next().getElement("mCounter-avg"), mapFor("a", "1", "b", "2"));
        assertAttributes(rows.next().getElement("mCounter-avg"), mapFor("c", "3", "d", "4"));

    }

    @Test
    public void testWithAbsolute() {

        Iterator<Row<Sample>> testSamples = new SampleRowsBuilder(new Resource("localhost"), MetricType.ABSOLUTE)
                .row(900000000).element("mAbsolute", 300)        // Thu Jul  9 11:00:00 CDT 1998
                .row(900000300).element("mAbsolute", 300)
                .row(900000600).element("mAbsolute", 300)
                .row(900000900).element("mAbsolute", 300)
                .row(900001200).element("mAbsolute", 300)
                .row(900001500).element("mAbsolute", 300)
                .row(900001800).element("mAbsolute", 300, mapFor("a", "1"))
                .row(900002100).element("mAbsolute", 300)
                .row(900002400).element("mAbsolute", 300, mapFor("b", "2"))
                .row(900002700).element("mAbsolute", 300)
                .row(900003000).element("mAbsolute", 300)
                .row(900003300).element("mAbsolute", 300)
                .row(900003600).element("mAbsolute", 300)
                .row(900003900).element("mAbsolute", 300)
                .row(900004200).element("mAbsolute", 300)
                .row(900004500).element("mAbsolute", 300)
                .row(900004800).element("mAbsolute", 300)
                .row(900005100).element("mAbsolute", 300, mapFor("c", "3"))
                .row(900005400).element("mAbsolute", 300)
                .row(900005700).element("mAbsolute", 300, mapFor("d", "4"))
                .row(900006000).element("mAbsolute", 300)
                .row(900006300).element("mAbsolute", 300)
                .row(900006600).element("mAbsolute", 300)
                .row(900006900).element("mAbsolute", 300)
                .row(900007200).element("mAbsolute", 300)        // Thu Jul  9 13:00:00 CDT 1998
                .build();
        
        ResultDescriptor rDescriptor = new ResultDescriptor(Duration.seconds(300))
            .datasource("mAbsolute-avg", "mAbsolute", Duration.seconds(600), AVERAGE).export("mAbsolute-avg");

        Iterator<Row<Measurement>> expected = new MeasurementRowsBuilder(new Resource("localhost"))
                .row(900003600).element("mAbsolute-avg", 1)
                .row(900007200).element("mAbsolute-avg", 1)
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
        assertAttributes(rows.next().getElement("mAbsolute-avg"), mapFor("a", "1", "b", "2"));
        assertAttributes(rows.next().getElement("mAbsolute-avg"), mapFor("c", "3", "d", "4"));

    }

    @Test
    public void testWithDerive() {

        Iterator<Row<Sample>> testSamples = new SampleRowsBuilder(new Resource("localhost"), MetricType.DERIVE)
                .row(900000000).element("mDerive",    0)        // Thu Jul  9 11:00:00 CDT 1998
                .row(900000300).element("mDerive",  300)
                .row(900000600).element("mDerive",  600)
                .row(900000900).element("mDerive",  900)
                .row(900001200).element("mDerive", 1200)
                .row(900001500).element("mDerive", 1500)
                .row(900001800).element("mDerive", 1800, mapFor("a", "1"))
                .row(900002100).element("mDerive", 2100)
                .row(900002400).element("mDerive", 2400, mapFor("b", "2"))
                .row(900002700).element("mDerive", 2700)
                .row(900003000).element("mDerive", 3000)
                .row(900003300).element("mDerive", 3300)
                .row(900003600).element("mDerive", 3600)
                .row(900003900).element("mDerive", 3900)
                .row(900004200).element("mDerive", 4200)
                .row(900004500).element("mDerive", 4500)
                .row(900004800).element("mDerive", 4800)
                .row(900005100).element("mDerive", 5100, mapFor("c", "3"))
                .row(900005400).element("mDerive", 5400)
                .row(900005700).element("mDerive", 5700, mapFor("d", "4"))
                .row(900006000).element("mDerive", 6000)
                .row(900006300).element("mDerive", 6300)
                .row(900006600).element("mDerive", 6600)
                .row(900006900).element("mDerive", 6900)
                .row(900007200).element("mDerive", 7200)        // Thu Jul  9 13:00:00 CDT 1998
                .build();

        ResultDescriptor rDescriptor = new ResultDescriptor(Duration.seconds(300))
            .datasource("mDerive-avg", "mDerive", Duration.seconds(600), AVERAGE).export("mDerive-avg");

        Iterator<Row<Measurement>> expected = new MeasurementRowsBuilder(new Resource("localhost"))
                .row(900003600).element("mDerive-avg", 1)
                .row(900007200).element("mDerive-avg", 1)
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
        assertAttributes(rows.next().getElement("mDerive-avg"), mapFor("a", "1", "b", "2"));
        assertAttributes(rows.next().getElement("mDerive-avg"), mapFor("c", "3", "d", "4"));

    }

}
