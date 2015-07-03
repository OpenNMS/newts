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
import static org.opennms.newts.api.Duration.seconds;
import static org.opennms.newts.api.query.StandardAggregationFunctions.AVERAGE;

import java.util.Iterator;

import org.junit.Test;
import org.opennms.newts.aggregate.Utils.MeasurementRowsBuilder;
import org.opennms.newts.api.Measurement;
import org.opennms.newts.api.Resource;
import org.opennms.newts.api.Results.Row;
import org.opennms.newts.api.query.ResultDescriptor;
import org.opennms.newts.api.query.ResultDescriptor.BinaryFunction;


public class ComputeTest {

    private static final BinaryFunction PLUS;
    private static final BinaryFunction DIVIDE;

    static {
        PLUS = new BinaryFunction() {
            private static final long serialVersionUID = 0L;

            @Override
            public double apply(double a, double b) {
                return a + b;
            }
        };
        
        DIVIDE = new BinaryFunction() {
            private static final long serialVersionUID = 0L;

            @Override
            public double apply(double a, double b) {
                return a / b;
            }
        };
    }

    @Test
    public void test() {

        Iterator<Row<Measurement>> testData = new MeasurementRowsBuilder(new Resource("localhost"))
                .row(300).element("in", 2).element("out", 2)
                .row(600).element("in", 6).element("out", 4)
                .build();

        ResultDescriptor rDescriptor = new ResultDescriptor()
                .datasource("in",  AVERAGE)
                .datasource("out", AVERAGE)
                .calculate("total", PLUS, "in", "out");

        Iterator<Row<Measurement>> expected = new MeasurementRowsBuilder(new Resource("localhost"))
                .row(300).element("in", 2).element("out", 2).element("total", 4)
                .row(600).element("in", 6).element("out", 4).element("total", 10)
                .build();

        Compute compute = new Compute(rDescriptor, testData);

        assertRowsEqual(expected, compute);

    }
    
    @Test
    public void testCalcOfCalc() {
        Iterator<Row<Measurement>> testData = new MeasurementRowsBuilder(new Resource("localhost"))
            .row(300).element("in", 20).element("out", 20)
            .row(600).element("in", 60).element("out", 40)
            .build();

        ResultDescriptor rDescriptor = new ResultDescriptor()
            .datasource("in", AVERAGE)
            .datasource("out", AVERAGE)
            .calculate("sum", PLUS, "in", "out")
            .calculate("tens", DIVIDE, "sum", "10")
            .export("tens")
        ;

        Iterator<Row<Measurement>> expected = new MeasurementRowsBuilder(new Resource("localhost"))
            .row(300).element("in", 20).element("out", 20).element("sum", 40).element("tens", 4)
            .row(600).element("in", 60).element("out", 40).element("sum", 100).element("tens", 10)
            .build();
        
        Compute compute = new Compute(rDescriptor, testData);

        assertRowsEqual(expected, compute);

    }

    @Test
    public void testExpressions() {
        Iterator<Row<Measurement>> testData = new MeasurementRowsBuilder(new Resource("localhost"))
            .row(300).element("in", 20).element("out", 20)
            .row(600).element("in", 60).element("out", 40)
            .build();

        ResultDescriptor rDescriptor = new ResultDescriptor()
            .datasource("in", "ifInOctets", seconds(600), AVERAGE)
            .datasource("out", "ifOutOctets", seconds(600), AVERAGE)
            .expression("sum", "in + out")
            .expression("diff", "in - out")
            .expression("ratio", "diff/sum")
            .export("ratio");

        Iterator<Row<Measurement>> expected = new MeasurementRowsBuilder(new Resource("localhost"))
            .row(300).element("in", 20).element("out", 20).element("sum", 40).element("diff", 0).element("ratio", 0)
            .row(600).element("in", 60).element("out", 40).element("sum", 100).element("diff", 20).element("ratio", 0.2)
            .build();
    
        Compute compute = new Compute(rDescriptor, testData);

        assertRowsEqual(expected, compute);

    }
}
