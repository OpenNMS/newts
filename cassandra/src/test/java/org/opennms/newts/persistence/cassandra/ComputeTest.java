package org.opennms.newts.persistence.cassandra;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.opennms.newts.api.Timestamp.fromEpochSeconds;
import static org.opennms.newts.api.query.StandardAggregationFunctions.AVERAGE;

import org.junit.Test;
import org.opennms.newts.api.Measurement;
import org.opennms.newts.api.Results;
import org.opennms.newts.api.Results.Row;
import org.opennms.newts.api.query.ResultDescriptor;
import org.opennms.newts.api.query.ResultDescriptor.BinaryFunction;


public class ComputeTest {

    @Test
    public void test() {

        Results<Measurement> testData = new Results<>();

        testData.addElement(new Measurement(fromEpochSeconds(300), "localhost", "in", 2));
        testData.addElement(new Measurement(fromEpochSeconds(300), "localhost", "out", 2));

        testData.addElement(new Measurement(fromEpochSeconds(600), "localhost", "in", 6));
        testData.addElement(new Measurement(fromEpochSeconds(600), "localhost", "out", 4));

        ResultDescriptor rDescriptor = new ResultDescriptor().datasource("in", AVERAGE).datasource("out", AVERAGE);

        BinaryFunction func = new BinaryFunction() {

            @Override
            public double apply(double a, double b) {
                return a + b;
            }
        };

        rDescriptor.calculate("total", func, "in", "out");

        Compute compute = new Compute(rDescriptor, testData.iterator());

        assertTrue("Insufficient results", compute.hasNext());

        Row<Measurement> row = compute.next();
        assertNotNull("Missing element \"total\"", row.getElement("total"));
        assertNotNull("Missing element \"in\"", row.getElement("in"));
        assertNotNull("Missing element \"out\"", row.getElement("out"));
        assertEquals(4, row.getElement("total").getValue(), 0.0d);
        assertEquals(2, row.getElement("in").getValue(), 0.0d);
        assertEquals(2, row.getElement("out").getValue(), 0.0d);

        assertTrue("Insufficient results", compute.hasNext());

        row = compute.next();
        assertNotNull("Missing element \"total\"", row.getElement("total"));
        assertNotNull("Missing element \"in\"", row.getElement("in"));
        assertNotNull("Missing element \"out\"", row.getElement("out"));
        assertEquals(10, row.getElement("total").getValue(), 0.0d);
        assertEquals(6, row.getElement("in").getValue(), 0.0d);
        assertEquals(4, row.getElement("out").getValue(), 0.0d);

        assertFalse("Too many results", compute.hasNext());

    }

}
