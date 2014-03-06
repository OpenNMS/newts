package org.opennms.newts.persistence.cassandra;


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


public class PrimaryDataTest {

    @Test
    public void testShortSamples() {

        // Samples occur prior to the nearest step interval boundary.
        Results<Sample> testData = new SampleRowsBuilder("localhost", MetricType.GAUGE)
                .row(000).element("m0", 0).element("m1", 1)
                .row(250).element("m0", 1).element("m1", 2)
                .row(550).element("m0", 2).element("m1", 3)
                .row(850).element("m0", 3).element("m1", 4)
                .build();

        // Minimal result descriptor
        ResultDescriptor rDescriptor = new ResultDescriptor().step(Duration.seconds(300))
                .datasource("m0", "m0", Duration.seconds(600), null).datasource("m1", "m1", Duration.seconds(600), null);

        // Expected results
        Results<Measurement> expected = new MeasurementRowsBuilder("localhost")
                .row(300).element("m0", 1.167).element("m1", 2.167)
                .row(600).element("m0", 2.167).element("m1", 3.167)
                .row(900).element("m0", 3.000).element("m1", 4.000)
                .build();

        PrimaryData primaryData = new PrimaryData(
                "localhost",
                Timestamp.fromEpochSeconds(300),
                Timestamp.fromEpochSeconds(900),
                rDescriptor,
                testData.iterator());

        assertRowsEqual(expected, primaryData);

    }

    @Test
    public void testSkippedSample() {

        // Sample m0 is missing at timestamp 550, (but interval does not exceed heartbeat).
        Results<Sample> testData = new SampleRowsBuilder("localhost", MetricType.GAUGE)
                .row(  0).element("m0", 0).element("m1", 1)
                .row(250).element("m0", 1).element("m1", 2)
                .row(550).element("m1", 3)
                .row(840).element("m0", 3).element("m1", 4)
                .build();

        // Minimal result descriptor
        ResultDescriptor rDescriptor = new ResultDescriptor().step(Duration.seconds(300))
                .datasource("m0", "m0", Duration.seconds(600), null).datasource("m1", "m1", Duration.seconds(600), null);

        // Expected results
        Results<Measurement> expected = new MeasurementRowsBuilder("localhost")
                .row(300).element("m0", 1.000).element("m1", 2.167)
                .row(600).element("m0", 3.000).element("m1", 3.167)
                .row(900).element("m0", 3.000).element("m1", 4.000)
                .build();

        PrimaryData primaryData = new PrimaryData(
                "localhost",
                Timestamp.fromEpochSeconds(300),
                Timestamp.fromEpochSeconds(900),
                rDescriptor,
                testData.iterator());

        assertRowsEqual(expected, primaryData);

    }

    @Test
    public void testManyToOneSamples() {

        // Element interval is less than step size.
        Results<Sample> testData = new SampleRowsBuilder("localhost", MetricType.GAUGE)
                .row(   0).element("m0", 0).element("m1", 1)
                .row( 300).element("m0", 1).element("m1", 2)
                .row( 600).element("m0", 2).element("m1", 3)
                .row( 900).element("m0", 3).element("m1", 4)
                .row(1200).element("m0", 4).element("m1", 5)
                .row(1500).element("m0", 5).element("m1", 6)
                .row(1800).element("m0", 6).element("m1", 7)
                .row(2100).element("m0", 7).element("m1", 8)
                .build();

        // Minimal result descriptor
        ResultDescriptor rDescriptor = new ResultDescriptor().step(Duration.seconds(900))
                .datasource("m0", "m0", Duration.seconds(1800), null).datasource("m1", "m1", Duration.seconds(1800), null);

        // Expected results
        Results<Measurement> expected = new MeasurementRowsBuilder("localhost")
                .row( 900).element("m0", 2).element("m1", 3)
                .row(1800).element("m0", 5).element("m1", 6)
                .build();

        PrimaryData primaryData = new PrimaryData(
                "localhost",
                Timestamp.fromEpochSeconds( 900),
                Timestamp.fromEpochSeconds(1800),
                rDescriptor,
                testData.iterator());

        assertRowsEqual(expected, primaryData);

    }

    @Test
    public void testOneToOneSamples() {

        // Samples perfectly correlate to step interval boundaries.
        Results<Sample> testData = new SampleRowsBuilder("localhost", MetricType.GAUGE)
                .row(   0).element("m0", 0).element("m1", 1)
                .row( 300).element("m0", 1).element("m1", 2)
                .row( 600).element("m0", 2).element("m1", 3)
                .row( 900).element("m0", 3).element("m1", 4)
                .build();

        // Minimal result descriptor
        ResultDescriptor rDescriptor = new ResultDescriptor().step(Duration.seconds(300))
                .datasource("m0", "m0", Duration.seconds(600), null).datasource("m1", "m1", Duration.seconds(600), null);

        // Expected results
        Results<Measurement> expected = new MeasurementRowsBuilder("localhost")
                .row(300).element("m0", 1).element("m1", 2)
                .row(600).element("m0", 2).element("m1", 3)
                .row(900).element("m0", 3).element("m1", 4)
                .build();

        PrimaryData primaryData = new PrimaryData(
                "localhost",
                Timestamp.fromEpochSeconds(300),
                Timestamp.fromEpochSeconds(900),
                rDescriptor,
                testData.iterator());

        assertRowsEqual(expected, primaryData);

    }

    @Test
    public void testOneToManySamples() {

        // Actual sample interval is smaller than step size; One sample is mapped to many measurements
        Results<Sample> testData = new SampleRowsBuilder("localhost", MetricType.GAUGE)
                .row(   0).element("m0", 0).element("m1", 1)
                .row( 900).element("m0", 1).element("m1", 2)
                .row(1800).element("m0", 2).element("m1", 3)
                .build();

        // Minimal result descriptor
        ResultDescriptor rDescriptor = new ResultDescriptor().step(Duration.seconds(300))
                .datasource("m0", "m0", Duration.seconds(1000), null).datasource("m1", "m1", Duration.seconds(1000), null);

        // Expected results
        Results<Measurement> expected = new MeasurementRowsBuilder("localhost")
                .row( 300).element("m0", 1).element("m1", 2)
                .row( 600).element("m0", 1).element("m1", 2)
                .row( 900).element("m0", 1).element("m1", 2)
                .row(1200).element("m0", 2).element("m1", 3)
                .row(1500).element("m0", 2).element("m1", 3)
                .row(1800).element("m0", 2).element("m1", 3)
                .build();

        PrimaryData primaryData = new PrimaryData(
                "localhost",
                Timestamp.fromEpochSeconds( 300),
                Timestamp.fromEpochSeconds(1800),
                rDescriptor,
                testData.iterator());

        assertRowsEqual(expected, primaryData);

    }

    @Test
    public void testLongSamples() {

        // Samples occur later-than (after) the step interval.
        Results<Sample> testData = new SampleRowsBuilder("localhost", MetricType.GAUGE)
                .row(  0).element("m0", 0).element("m1", 1)
                .row(350).element("m0", 1).element("m1", 2)
                .row(650).element("m0", 2).element("m1", 3)
                .row(950).element("m0", 3).element("m1", 4)
                .build();

        // Minimal result descriptor
        ResultDescriptor rDescriptor = new ResultDescriptor().step(Duration.seconds(300))
                .datasource("m0", "m0", Duration.seconds(600), null).datasource("m1", "m1", Duration.seconds(600), null);

        // Expected results
        Results<Measurement> expected = new MeasurementRowsBuilder("localhost")
                .row( 300).element("m0", 1.000).element("m1", 2.000)
                .row( 600).element("m0", 1.833).element("m1", 2.833)
                .row( 900).element("m0", 2.833).element("m1", 3.833)
                .build();

        PrimaryData primaryData = new PrimaryData(
                "localhost",
                Timestamp.fromEpochSeconds(300),
                Timestamp.fromEpochSeconds(900),
                rDescriptor,
                testData.iterator());

        assertRowsEqual(expected, primaryData);

    }

    @Test
    public void testHeartbeat() {

        // Sample interval of 600 seconds (m1) exceeds heartbeat of 601
        Results<Sample> testData = new SampleRowsBuilder("localhost", MetricType.GAUGE)
                .row(  0).element("m0", 0).element("m1", 1)
                .row(300).element("m0", 1).element("m1", 2)
                .row(600).element("m0", 2)
                .row(900).element("m0", 3).element("m1", 4)
                .build();

        // Minimal result descriptor
        ResultDescriptor rDescriptor = new ResultDescriptor().step(Duration.seconds(300))
                .datasource("m0", "m0", Duration.seconds(601), null).datasource("m1", "m1", Duration.seconds(601), null);

        // Expected results
        Results<Measurement> expected = new MeasurementRowsBuilder("localhost")
                .row( 300).element("m0", 1).element("m1", 2)
                .row( 600).element("m0", 2).element("m1", Double.NaN)
                .row( 900).element("m0", 3).element("m1", 4)
                .build();

        PrimaryData primaryData = new PrimaryData(
                "localhost",
                Timestamp.fromEpochSeconds(300),
                Timestamp.fromEpochSeconds(900),
                rDescriptor,
                testData.iterator());

        assertRowsEqual(expected, primaryData);

    }

}