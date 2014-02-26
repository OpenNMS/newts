package org.opennms.newts.persistence.cassandra;


import static org.opennms.newts.persistence.cassandra.Utils.assertRowsEqual;
import static org.opennms.newts.persistence.cassandra.Utils.getTestCase;

import org.junit.Test;
import org.opennms.newts.api.query.Datasource.StandardAggregationFunctions;
import org.opennms.newts.api.query.ResultDescriptor;


public class PrimaryDataTest {

    private PrimaryData getIterator(XMLTestCase testCase) {

        ResultDescriptor resultDescriptor = new ResultDescriptor(testCase.getInterval());

        for (String name : testCase.getMetrics()) {
            resultDescriptor.datasource(name, name, testCase.getHeartbeat(), StandardAggregationFunctions.AVERAGE);
        }

        return new PrimaryData(
                resultDescriptor,
                testCase.getResource(),
                testCase.getStart(),
                testCase.getEnd(),
                testCase.getTestDataAsSamples().iterator());
    }

    private void execute(XMLTestCase testCase) {
        assertRowsEqual(testCase.getExpected(), getIterator(testCase));
    }

    @Test
    public void testShortSamples() {
        execute(getTestCase("primary_data/shortSamples.xml"));
    }

    @Test
    public void testSkippedSample() {
        execute(getTestCase("primary_data/skippedSample.xml"));
    }

    @Test
    public void testManyToOneSamples() {
        execute(getTestCase("primary_data/manyToOne.xml"));
    }

    @Test
    public void testOneToOneSamples() {
        execute(getTestCase("primary_data/oneToOne.xml"));
    }

    @Test
    public void testOneToManySamples() {
        execute(getTestCase("primary_data/oneToMany.xml"));
    }

    @Test
    public void testLongSamples() {
        execute(getTestCase("primary_data/longSamples.xml"));
    }

    @Test
    public void testHeartbeat() {
        execute(getTestCase("primary_data/heartbeat.xml"));
    }

}
