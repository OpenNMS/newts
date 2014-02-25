package org.opennms.newts.persistence.cassandra;


import static org.opennms.newts.persistence.cassandra.Utils.assertRowsEqual;
import static org.opennms.newts.persistence.cassandra.Utils.getTestCase;

import javax.xml.bind.JAXBException;

import org.junit.Ignore;
import org.junit.Test;
import org.opennms.newts.api.query.Datasource.StandardAggregationFunctions;
import org.opennms.newts.api.query.ResultDescriptor;


public class PrimaryDataTest {

    private PrimaryData getIterator(XMLTestCase testCase) {

        ResultDescriptor resultDescriptor = new ResultDescriptor(testCase.getInterval());

        for (String name : testCase.getMetrics()) {
            resultDescriptor.datasource(name, StandardAggregationFunctions.AVERAGE);
        }

        return new PrimaryData(
                resultDescriptor,
                testCase.getResource(),
                testCase.getStart(),
                testCase.getEnd(),
                testCase.getSamples().iterator());
    }

    private void execute(XMLTestCase testCase) {
        assertRowsEqual(testCase.getMeasurements(), getIterator(testCase));
    }

    @Test
    public void testShortSamples() {
        execute(getTestCase("primaryData/shortSamples.xml"));
    }

    @Test
    public void testSkippedSample() {
        execute(getTestCase("primaryData/skippedSample.xml"));
    }

    @Test
    public void testManyToOneSamples() {
        execute(getTestCase("primaryData/manyToOne.xml"));
    }

    @Test
    public void testOneToOneSamples() {
        execute(getTestCase("primaryData/oneToOne.xml"));
    }

    @Test
    @Ignore
    public void testOneToManySamples() {
        execute(getTestCase("primaryData/oneToMany.xml"));
    }

    @Test
    public void testLongSamples() {
        execute(getTestCase("primaryData/longSamples.xml"));
    }

    @Test
    @Ignore
    public void testHeartbeat() {
        execute(getTestCase("primaryData/heartbeat.xml"));
    }

}
