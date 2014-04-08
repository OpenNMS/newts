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


import static java.lang.Double.NaN;
import static org.opennms.newts.api.query.StandardAggregationFunctions.AVERAGE;
import static org.opennms.newts.api.query.StandardAggregationFunctions.MAX;
import static org.opennms.newts.api.query.StandardAggregationFunctions.MIN;
import static org.opennms.newts.persistence.cassandra.Utils.assertRowsEqual;

import java.util.Iterator;

import org.junit.Test;
import org.opennms.newts.api.Duration;
import org.opennms.newts.api.Measurement;
import org.opennms.newts.api.Results.Row;
import org.opennms.newts.api.Timestamp;
import org.opennms.newts.api.query.ResultDescriptor;
import org.opennms.newts.persistence.cassandra.Utils.MeasurementRowsBuilder;


public class AggregationTest {

    @Test
    public void test() {

        Iterator<Row<Measurement>> testData = new MeasurementRowsBuilder("localhost")
                .row(   1).element("m0", 1)
                .row( 300).element("m0", 1)
                .row( 600).element("m0", 1)
                .row( 900).element("m0", 1)
                .row(1200).element("m0", 1)
                .row(1500).element("m0", 1)
                .row(1800).element("m0", 3)
                .row(2100).element("m0", 3)
                .row(2400).element("m0", 3)
                .row(2700).element("m0", 3)
                .row(3000).element("m0", 3)
                .row(3300).element("m0", 3)
                .build();

        ResultDescriptor rDescriptor = new ResultDescriptor(Duration.seconds(300))
                .datasource("m0-avg", "m0", Duration.seconds(600), AVERAGE)
                .datasource("m0-min", "m0", Duration.seconds(600), MIN)
                .datasource("m0-max", "m0", Duration.seconds(600), MAX);

        Iterator<Row<Measurement>> expected = new MeasurementRowsBuilder("localhost")
                .row(   0).element("m0-avg", NaN).element("m0-min", NaN).element("m0-max", NaN)
                .row(3600).element("m0-avg",   2).element("m0-min",   1).element("m0-max",   3)
                .build();

        Aggregation aggregation = new Aggregation(
                "localhost",
                Timestamp.fromEpochSeconds(   1),
                Timestamp.fromEpochSeconds(3300),
                rDescriptor,
                Duration.minutes(60),
                testData);

        assertRowsEqual(expected, aggregation);

    }

}
