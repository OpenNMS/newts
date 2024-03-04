/*
 * Copyright 2014-2024, The OpenNMS Group
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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.opennms.newts.aggregate.Utils.assertAttributes;
import static org.opennms.newts.aggregate.Utils.mapFor;
import static org.opennms.newts.api.Timestamp.fromEpochSeconds;
import static org.opennms.newts.api.query.StandardAggregationFunctions.AVERAGE;
import static org.opennms.newts.api.query.StandardAggregationFunctions.MAX;
import static org.opennms.newts.api.query.StandardAggregationFunctions.MIN;

import java.util.Iterator;

import org.junit.Test;
import org.opennms.newts.aggregate.Utils.MeasurementRowsBuilder;
import org.opennms.newts.api.Duration;
import org.opennms.newts.api.Measurement;
import org.opennms.newts.api.Resource;
import org.opennms.newts.api.Results.Row;
import org.opennms.newts.api.Timestamp;
import org.opennms.newts.api.query.ResultDescriptor;

public class AggregationAttributesTest {

    @Test
    public void test() {

        Iterator<Row<Measurement>> testData = new MeasurementRowsBuilder(new Resource("localhost"))
                .row(   1).element("m0", 1, mapFor("a", "1"))
                .row( 300).element("m0", 1)
                .row( 600).element("m0", 1)
                .row( 900).element("m0", 1)
                .row(1200).element("m0", 1)
                .row(1500).element("m0", 1)
                .row(1800).element("m0", 3, mapFor("b", "1"))
                .row(2100).element("m0", 3)
                .row(2400).element("m0", 3, mapFor("a", "2"))
                .row(2700).element("m0", 3)
                .row(3000).element("m0", 3)
                .row(3300).element("m0", 3)
                .build();

        ResultDescriptor rDescriptor = new ResultDescriptor(Duration.seconds(300))
                .datasource("m0-avg", "m0", Duration.seconds(600), AVERAGE)
                .datasource("m0-min", "m0", Duration.seconds(600), MIN)
                .datasource("m0-max", "m0", Duration.seconds(600), MAX);

        Aggregation aggregation = new Aggregation(
                new Resource("localhost"),
                Timestamp.fromEpochSeconds(   1),
                Timestamp.fromEpochSeconds(3300),
                rDescriptor,
                Duration.minutes(60),
                testData);

        Row<Measurement> row = aggregation.next();
        assertThat(row.getTimestamp(), equalTo(fromEpochSeconds(0)));
        assertThat(row.getElement("m0-avg").getAttributes(), nullValue());

        row = aggregation.next();
        assertThat(row.getTimestamp(), equalTo(fromEpochSeconds(3600)));
        assertAttributes(row.getElement("m0-avg"), mapFor("a", "2", "b", "1"));

    }

}
