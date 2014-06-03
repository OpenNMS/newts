package org.opennms.newts.persistence.leveldb;


import static org.opennms.newts.api.query.StandardAggregationFunctions.AVERAGE;
import static org.opennms.newts.persistence.leveldb.Utils.assertRowsEqual;

import java.util.Iterator;
import java.util.List;

import org.junit.Test;
import org.opennms.newts.api.Duration;
import org.opennms.newts.api.Measurement;
import org.opennms.newts.api.MetricType;
import org.opennms.newts.api.Results;
import org.opennms.newts.api.Results.Row;
import org.opennms.newts.api.Sample;
import org.opennms.newts.api.Timestamp;
import org.opennms.newts.api.query.ResultDescriptor;
import org.opennms.newts.persistence.leveldb.Utils.MeasurementRowsBuilder;
import org.opennms.newts.persistence.leveldb.Utils.SampleRowsBuilder;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;

public class InsertSelectMeasurementsITCase extends AbstractLeveldbTestCase {

    private void writeSamples(Iterator<Row<Sample>> samples) {
        List<Sample> writeSamples = Lists.newArrayList();

        while (samples.hasNext()) {
            writeSamples.addAll(samples.next().getElements());
        }

        getRepository().insert(writeSamples);
    }

    @Test
    public void testMeasurements() {

        Iterator<Row<Sample>> testSamples = new SampleRowsBuilder("localhost", MetricType.GAUGE)
                .row(900000000).element("m0", 1)        // Thu Jul  9 11:00:00 CDT 1998
                .row(900000300).element("m0", 1)
                .row(900000600).element("m0", 1)
                .row(900000900).element("m0", 1)
                .row(900001200).element("m0", 1)
                .row(900001500).element("m0", 1)
                .row(900001800).element("m0", 1)
                .row(900002100).element("m0", 3)
                .row(900002400).element("m0", 3)
                .row(900002700).element("m0", 3)
                .row(900003000).element("m0", 3)
                .row(900003300).element("m0", 3)
                .row(900003600).element("m0", 3)
                .row(900003900).element("m0", 1)
                .row(900004200).element("m0", 1)
                .row(900004500).element("m0", 1)
                .row(900004800).element("m0", 1)
                .row(900005100).element("m0", 1)
                .row(900005400).element("m0", 1)
                .row(900005700).element("m0", 3)
                .row(900006000).element("m0", 3)
                .row(900006300).element("m0", 3)
                .row(900006600).element("m0", 3)
                .row(900006900).element("m0", 3)
                .row(900007200).element("m0", 3)        // Thu Jul  9 13:00:00 CDT 1998
                .build();

        ResultDescriptor rDescriptor = new ResultDescriptor(Duration.seconds(300))
            .datasource("m0-avg", "m0", Duration.seconds(600), AVERAGE).export("m0-avg");

        Iterator<Row<Measurement>> expected = new MeasurementRowsBuilder("localhost")
                .row(900003600).element("m0-avg", 2)
                .row(900007200).element("m0-avg", 2)
                .build();

        writeSamples(testSamples);

        Results<Measurement> results = getRepository().select(
                "localhost",
                Optional.of(Timestamp.fromEpochSeconds(900003600)),
                Optional.of(Timestamp.fromEpochSeconds(900007200)),
                rDescriptor,
                Duration.minutes(60));

        assertRowsEqual(expected, results.iterator());

    }

}
