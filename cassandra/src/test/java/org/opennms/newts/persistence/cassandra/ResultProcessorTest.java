package org.opennms.newts.persistence.cassandra;

import static org.opennms.newts.api.query.StandardAggregationFunctions.AVERAGE;
import static org.opennms.newts.persistence.cassandra.Utils.assertRowsEqual;

import org.junit.Test;
import org.opennms.newts.api.Duration;
import org.opennms.newts.api.Measurement;
import org.opennms.newts.api.MetricType;
import org.opennms.newts.api.Results;
import org.opennms.newts.api.Sample;
import org.opennms.newts.api.Timestamp;
import org.opennms.newts.api.query.ResultDescriptor;
import org.opennms.newts.persistence.cassandra.Utils.MeasurementRowsBuilder;
import org.opennms.newts.persistence.cassandra.Utils.SampleRowsBuilder;

public class ResultProcessorTest {

    @Test
    public void test() {

        Results<Sample> testData = new SampleRowsBuilder("localhost", MetricType.GAUGE)
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

        Results<Measurement> expected = new MeasurementRowsBuilder("localhost")
                .row(900003600).element("m0-avg", 2.0)
                .row(900007200).element("m0-avg", 2.0)
                .build();

        ResultProcessor processor = new ResultProcessor(
                "localhost",
                Timestamp.fromEpochSeconds(900003600),
                Timestamp.fromEpochSeconds(900007200),
                rDescriptor,
                Duration.minutes(60));

        assertRowsEqual(expected, processor.process(testData.iterator()));

    }

}
