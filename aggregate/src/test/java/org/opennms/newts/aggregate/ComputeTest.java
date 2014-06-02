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
import static org.opennms.newts.api.query.StandardAggregationFunctions.AVERAGE;

import java.util.Iterator;

import org.junit.Test;
import org.opennms.newts.aggregate.Utils.MeasurementRowsBuilder;
import org.opennms.newts.api.Measurement;
import org.opennms.newts.api.Results.Row;
import org.opennms.newts.api.query.ResultDescriptor;
import org.opennms.newts.api.query.ResultDescriptor.BinaryFunction;


public class ComputeTest {

    private static final BinaryFunction ADD;

    static {
        ADD = new BinaryFunction() {

            @Override
            public double apply(double a, double b) {
                return a + b;
            }
        };
    }

    @Test
    public void test() {

        Iterator<Row<Measurement>> testData = new MeasurementRowsBuilder("localhost")
                .row(300).element("in", 2).element("out", 2)
                .row(600).element("in", 6).element("out", 4)
                .build();

        ResultDescriptor rDescriptor = new ResultDescriptor()
                .datasource("in",  AVERAGE)
                .datasource("out", AVERAGE)
                .calculate("total", ADD, "in", "out");

        Iterator<Row<Measurement>> expected = new MeasurementRowsBuilder("localhost")
                .row(300).element("in", 2).element("out", 2).element("total", 4)
                .row(600).element("in", 6).element("out", 4).element("total", 10)
                .build();

        Compute compute = new Compute(rDescriptor, testData);

        assertRowsEqual(expected, compute);

    }

}
