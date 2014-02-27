package org.opennms.newts.persistence.cassandra;


import org.junit.Test;
import org.opennms.newts.api.query.Datasource.StandardAggregationFunctions;
import org.opennms.newts.api.query.ResultDescriptor;


public class PrimaryDataTest extends AbstractXMLTestCase {

    PrimaryData getIterator(XMLTestSpecification testCase) {

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

    @Test
    public void testShortSamples() {
        execute("primary_data/shortSamples.xml");
    }

    @Test
    public void testSkippedSample() {
        execute("primary_data/skippedSample.xml");
    }

    @Test
    public void testManyToOneSamples() {
        execute("primary_data/manyToOne.xml");
    }

    @Test
    public void testOneToOneSamples() {
        execute("primary_data/oneToOne.xml");
    }

    @Test
    public void testOneToManySamples() {
        execute("primary_data/oneToMany.xml");
    }

    @Test
    public void testLongSamples() {
        execute("primary_data/longSamples.xml");
    }

    @Test
    public void testHeartbeat() {
        execute("primary_data/heartbeat.xml");
    }

}
