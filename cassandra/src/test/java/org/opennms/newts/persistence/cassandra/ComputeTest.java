package org.opennms.newts.persistence.cassandra;


import static org.opennms.newts.api.query.StandardAggregationFunctions.AVERAGE;
import static org.opennms.newts.persistence.cassandra.Utils.assertRowsEqual;

import org.junit.Test;
import org.opennms.newts.api.Measurement;
import org.opennms.newts.api.Results;
import org.opennms.newts.api.query.ResultDescriptor;
import org.opennms.newts.api.query.ResultDescriptor.BinaryFunction;
import org.opennms.newts.persistence.cassandra.Utils.MeasurementRowsBuilder;


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

        Results<Measurement> testData = new MeasurementRowsBuilder("localhost")
                .row(300).element("in", 2).element("out", 2)
                .row(600).element("in", 6).element("out", 4)
                .build();

        ResultDescriptor rDescriptor = new ResultDescriptor()
                .datasource("in",  AVERAGE)
                .datasource("out", AVERAGE)
                .calculate("total", ADD, "in", "out");

        Results<Measurement> expected = new MeasurementRowsBuilder("localhost")
                .row(300).element("in", 2).element("out", 2).element("total", 4)
                .row(600).element("in", 6).element("out", 4).element("total", 10)
                .build();

        Compute compute = new Compute(rDescriptor, testData.iterator());

        assertRowsEqual(expected, compute);

    }

}
