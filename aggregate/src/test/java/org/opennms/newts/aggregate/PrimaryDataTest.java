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
package org.opennms.newts.aggregate;


import static org.opennms.newts.aggregate.Utils.assertRowsEqual;

import java.util.Iterator;

import org.junit.Test;
import org.opennms.newts.aggregate.Utils.MeasurementRowsBuilder;
import org.opennms.newts.aggregate.Utils.SampleRowsBuilder;
import org.opennms.newts.api.Duration;
import org.opennms.newts.api.Measurement;
import org.opennms.newts.api.MetricType;
import org.opennms.newts.api.Resource;
import org.opennms.newts.api.Results.Row;
import org.opennms.newts.api.Sample;
import org.opennms.newts.api.Timestamp;
import org.opennms.newts.api.query.ResultDescriptor;


public class PrimaryDataTest {

    @Test
    public void testLeadingSamplesMiss() {

        // Missing a couple leading samples
        Iterator<Row<Sample>> testData = new SampleRowsBuilder(new Resource("localhost"), MetricType.GAUGE)
                .row(900000300).element("m0", 0)
                .row(900000600).element("m0", 1)
                .row(900000900).element("m0", 2)
                .row(900001200).element("m0", 3)
                .build();

        ResultDescriptor rDescriptor = new ResultDescriptor(Duration.seconds(300))
                .datasource("m0", "m0", Duration.seconds(600), null);

        // Expected results
        Iterator<Row<Measurement>> expected = new MeasurementRowsBuilder(new Resource("localhost"))
                .row(900000000).element("m0", Double.NaN)
                .row(900000300).element("m0", Double.NaN)
                .row(900000600).element("m0", 1)
                .row(900000900).element("m0", 2)
                .row(900001200).element("m0", 3)
                .build();

        PrimaryData primaryData = new PrimaryData(
                new Resource("localhost"),
                Timestamp.fromEpochSeconds(900000000),
                Timestamp.fromEpochSeconds(900001200),
                rDescriptor,
                testData);

        assertRowsEqual(expected, primaryData);

    }

    @Test
    public void testShortSamples() {

        // Samples occur prior to the nearest step interval boundary.
        Iterator<Row<Sample>> testData = new SampleRowsBuilder(new Resource("localhost"), MetricType.GAUGE)
                .row(000).element("m0", 0).element("m1", 1)
                .row(250).element("m0", 1).element("m1", 2)
                .row(550).element("m0", 2).element("m1", 3)
                .row(850).element("m0", 3).element("m1", 4)
                .build();

        // Minimal result descriptor
        ResultDescriptor rDescriptor = new ResultDescriptor().step(Duration.seconds(300))
                .datasource("m0", "m0", Duration.seconds(600), null).datasource("m1", "m1", Duration.seconds(600), null);

        // Expected results
        Iterator<Row<Measurement>> expected = new MeasurementRowsBuilder(new Resource("localhost"))
                .row(300).element("m0", 1.16666667).element("m1", 2.16666667)
                .row(600).element("m0", 2.16666667).element("m1", 3.16666667)
                .row(900).element("m0",        3.0).element("m1",        4.0)
                .build();

        PrimaryData primaryData = new PrimaryData(
                new Resource("localhost"),
                Timestamp.fromEpochSeconds(300),
                Timestamp.fromEpochSeconds(900),
                rDescriptor,
                testData);

        assertRowsEqual(expected, primaryData);

    }

    @Test
    public void testVeryShortSamples() {

        // Samples occur prior to the nearest step interval boundary.
        Iterator<Row<Sample>> testData = new SampleRowsBuilder(new Resource("localhost"), MetricType.GAUGE)
                .row(000).element("m0", 0).element("m1", 1)
                .row(250).element("m0", 1).element("m1", 2)
                .row(550).element("m0", 2).element("m1", 3)
                .row(850).element("m0", 3).element("m1", 4)
                .build();

        // Minimal result descriptor
        ResultDescriptor rDescriptor = new ResultDescriptor().step(Duration.seconds(300))
                .datasource("m0", "m0", Duration.seconds(600), null).datasource("m1", "m1", Duration.seconds(600), null);

        // Expected results
        Iterator<Row<Measurement>> expected = new MeasurementRowsBuilder(new Resource("localhost"))
                .row(300).element("m0", 1.16666667).element("m1", 2.16666667)
                .row(600).element("m0", 2.16666667).element("m1", 3.16666667)
                .row(900).element("m0", 3.0).element("m1", 4.0)
                .row(1200).element("m0", Double.NaN).element("m1", Double.NaN)
                .row(1500).element("m0", Double.NaN).element("m1", Double.NaN)
                .build();

        PrimaryData primaryData = new PrimaryData(
                new Resource("localhost"),
                Timestamp.fromEpochSeconds(300),
                Timestamp.fromEpochSeconds(1500),
                rDescriptor,
                testData);

        assertRowsEqual(expected, primaryData);

    }


    @Test
    public void testSkippedSample() {

        // Sample m0 is missing at timestamp 550, (but interval does not exceed heartbeat).
        Iterator<Row<Sample>> testData = new SampleRowsBuilder(new Resource("localhost"), MetricType.GAUGE)
                .row(  0).element("m0", 0).element("m1", 1)
                .row(250).element("m0", 1).element("m1", 2)
                .row(550).element("m1", 3)
                .row(840).element("m0", 3).element("m1", 4)
                .build();

        // Minimal result descriptor
        ResultDescriptor rDescriptor = new ResultDescriptor().step(Duration.seconds(300))
                .datasource("m0", "m0", Duration.seconds(600), null).datasource("m1", "m1", Duration.seconds(600), null);

        // Expected results
        Iterator<Row<Measurement>> expected = new MeasurementRowsBuilder(new Resource("localhost"))
                .row(300).element("m0", 1.33333333).element("m1", 2.16666667)
                .row(600).element("m0", 3.00000000).element("m1", 3.16666667)
                .row(900).element("m0", 3.00000000).element("m1", 4.00000000)
                .build();

        PrimaryData primaryData = new PrimaryData(
                new Resource("localhost"),
                Timestamp.fromEpochSeconds(300),
                Timestamp.fromEpochSeconds(900),
                rDescriptor,
                testData);

        assertRowsEqual(expected, primaryData);

    }

    @Test
    public void testManyToOneSamples() {

        // Element interval is less than step size.
        Iterator<Row<Sample>> testData = new SampleRowsBuilder(new Resource("localhost"), MetricType.GAUGE)
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
        Iterator<Row<Measurement>> expected = new MeasurementRowsBuilder(new Resource("localhost"))
                .row( 900).element("m0", 2).element("m1", 3)
                .row(1800).element("m0", 5).element("m1", 6)
                .build();

        PrimaryData primaryData = new PrimaryData(
                new Resource("localhost"),
                Timestamp.fromEpochSeconds( 900),
                Timestamp.fromEpochSeconds(1800),
                rDescriptor,
                testData);

        assertRowsEqual(expected, primaryData);

    }

    @Test
    public void testOneToOneSamples() {

        // Samples perfectly correlate to step interval boundaries.
        Iterator<Row<Sample>> testData = new SampleRowsBuilder(new Resource("localhost"), MetricType.GAUGE)
                .row(   0).element("m0", 0).element("m1", 1)
                .row( 300).element("m0", 1).element("m1", 2)
                .row( 600).element("m0", 2).element("m1", 3)
                .row( 900).element("m0", 3).element("m1", 4)
                .build();

        // Minimal result descriptor
        ResultDescriptor rDescriptor = new ResultDescriptor().step(Duration.seconds(300))
                .datasource("m0", "m0", Duration.seconds(600), null).datasource("m1", "m1", Duration.seconds(600), null);

        // Expected results
        Iterator<Row<Measurement>> expected = new MeasurementRowsBuilder(new Resource("localhost"))
                .row(300).element("m0", 1).element("m1", 2)
                .row(600).element("m0", 2).element("m1", 3)
                .row(900).element("m0", 3).element("m1", 4)
                .build();

        PrimaryData primaryData = new PrimaryData(
                new Resource("localhost"),
                Timestamp.fromEpochSeconds(300),
                Timestamp.fromEpochSeconds(900),
                rDescriptor,
                testData);

        assertRowsEqual(expected, primaryData);

    }

    @Test
    public void testOneToManySamples() {

        // Actual sample interval is smaller than step size; One sample is mapped to many measurements
        Iterator<Row<Sample>> testData = new SampleRowsBuilder(new Resource("localhost"), MetricType.GAUGE)
                .row(   0).element("m0", 0).element("m1", 1)
                .row( 900).element("m0", 1).element("m1", 2)
                .row(1800).element("m0", 2).element("m1", 3)
                .build();

        // Minimal result descriptor
        ResultDescriptor rDescriptor = new ResultDescriptor().step(Duration.seconds(300))
                .datasource("m0", "m0", Duration.seconds(1000), null).datasource("m1", "m1", Duration.seconds(1000), null);

        // Expected results
        Iterator<Row<Measurement>> expected = new MeasurementRowsBuilder(new Resource("localhost"))
                .row( 300).element("m0", 1).element("m1", 2)
                .row( 600).element("m0", 1).element("m1", 2)
                .row( 900).element("m0", 1).element("m1", 2)
                .row(1200).element("m0", 2).element("m1", 3)
                .row(1500).element("m0", 2).element("m1", 3)
                .row(1800).element("m0", 2).element("m1", 3)
                .build();

        PrimaryData primaryData = new PrimaryData(
                new Resource("localhost"),
                Timestamp.fromEpochSeconds( 300),
                Timestamp.fromEpochSeconds(1800),
                rDescriptor,
                testData);

        assertRowsEqual(expected, primaryData);

    }

    @Test
    public void testLongSamples() {

        // Samples occur later-than (after) the step interval.
        Iterator<Row<Sample>> testData = new SampleRowsBuilder(new Resource("localhost"), MetricType.GAUGE)
                .row(  0).element("m0", 0).element("m1", 1)
                .row(350).element("m0", 1).element("m1", 2)
                .row(650).element("m0", 2).element("m1", 3)
                .row(950).element("m0", 3).element("m1", 4)
                .build();

        // Minimal result descriptor
        ResultDescriptor rDescriptor = new ResultDescriptor().step(Duration.seconds(300))
                .datasource("m0", "m0", Duration.seconds(600), null).datasource("m1", "m1", Duration.seconds(600), null);

        // Expected results
        Iterator<Row<Measurement>> expected = new MeasurementRowsBuilder(new Resource("localhost"))
                .row( 300).element("m0",        1.0).element("m1",        2.0)
                .row( 600).element("m0", 1.83333333).element("m1", 2.83333333)
                .row( 900).element("m0", 2.83333333).element("m1", 3.83333333)
                .build();

        PrimaryData primaryData = new PrimaryData(
                new Resource("localhost"),
                Timestamp.fromEpochSeconds(300),
                Timestamp.fromEpochSeconds(900),
                rDescriptor,
                testData);

        assertRowsEqual(expected, primaryData);

    }

    @Test
    public void testHeartbeatOneSample() {

        // Sample interval of 600 seconds (m1) exceeds heartbeat of 601
        Iterator<Row<Sample>> testData = new SampleRowsBuilder(new Resource("localhost"), MetricType.GAUGE)
                .row(  0).element("m1", 1)
                .row(300).element("m1", 2)
                .row(900).element("m1", 4)
                .build();

        // Minimal result descriptor
        ResultDescriptor rDescriptor = new ResultDescriptor().step(Duration.seconds(300))
                .datasource("m1", "m1", Duration.seconds(601), null);

        // Expected results
        Iterator<Row<Measurement>> expected = new MeasurementRowsBuilder(new Resource("localhost"))
                .row( 300)
                    .element("m1", 2)
                .row( 600)
                    .element("m1", 4)
                .row( 900)
                    .element("m1", 4)
                .build();

        PrimaryData primaryData = new PrimaryData(
                new Resource("localhost"),
                Timestamp.fromEpochSeconds(300),
                Timestamp.fromEpochSeconds(900),
                rDescriptor,
                testData);

        assertRowsEqual(expected, primaryData);

    }
    
    @Test
    public void testHeartbeatTwoSamples() {

        // Sample interval of 600 seconds (m1) exceeds heartbeat of 601
        Iterator<Row<Sample>> testData = new SampleRowsBuilder(new Resource("localhost"), MetricType.GAUGE)
                .row(  0)
                    .element("m0", 0)
                    .element("m1", 1)
                .row(300)
                    .element("m0", 1)
                    .element("m1", 2)
                .row(600)
                    .element("m0", 2)
                    // missing entry for m1
                .row(900)
                    .element("m0", 3)
                    .element("m1", 4)
                .build();

        // Minimal result descriptor
        ResultDescriptor rDescriptor = new ResultDescriptor().step(Duration.seconds(300))
                .datasource("m0", "m0", Duration.seconds(601), null)
                .datasource("m1", "m1", Duration.seconds(601), null);

        // Expected results
        Iterator<Row<Measurement>> expected = new MeasurementRowsBuilder(new Resource("localhost"))
                .row( 300)
                    .element("m0", 1)
                    .element("m1", 2)
                .row( 600)
                    .element("m0", 2)
                    .element("m1", 4)
                .row( 900)
                    .element("m0", 3)
                    .element("m1", 4)
                .build();

        PrimaryData primaryData = new PrimaryData(
                new Resource("localhost"),
                Timestamp.fromEpochSeconds(300),
                Timestamp.fromEpochSeconds(900),
                rDescriptor,
                testData);

        assertRowsEqual(expected, primaryData);

    }

    @Test
    public void testHeartbeatNaNs() {

        // Test for NEWTS-70
        Iterator<Row<Sample>> testData = new SampleRowsBuilder(new Resource("localhost"), MetricType.GAUGE)
                .row(   0).element("m1", 1)
                .row( 300).element("m1", 2)
                .row(1800).element("m1", 8)
                .build();

        ResultDescriptor rDescriptor = new ResultDescriptor().step(Duration.seconds(300))
                .datasource("m1", "m1", Duration.seconds(600), null);

        // Expected results
        Iterator<Row<Measurement>> expected = new MeasurementRowsBuilder(new Resource("localhost"))
                .row( 300)
                    .element("m1", 2)
                .row( 600)
                    .element("m1", Double.NaN)
                .row( 900)
                    .element("m1", Double.NaN)
                .row(1200)
                    .element("m1", Double.NaN)
                .row(1500)
                    .element("m1", Double.NaN)
                .row(1800)
                    .element("m1", Double.NaN)
                .build();

        PrimaryData primaryData = new PrimaryData(
                new Resource("localhost"),
                Timestamp.fromEpochSeconds(300),
                Timestamp.fromEpochSeconds(1800),
                rDescriptor,
                testData);

        assertRowsEqual(expected, primaryData);

    }

    @Test(expected = UnsupportedOperationException.class)
    public void testRemove() {
	new PrimaryData(new Resource("localhost"), Timestamp.fromEpochSeconds(0), Timestamp.fromEpochSeconds(1000), new ResultDescriptor(), new SampleRowsBuilder(new Resource("localhost"), MetricType.GAUGE).row(0).element("m0", 0).row(100).element("m0", 1).build()).remove();
    }
}
