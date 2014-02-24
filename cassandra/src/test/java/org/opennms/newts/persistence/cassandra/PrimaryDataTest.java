package org.opennms.newts.persistence.cassandra;


import static org.opennms.newts.persistence.cassandra.Utils.assertRowsEqual;
import static org.opennms.newts.persistence.cassandra.Utils.getHeartbeats;
import static org.opennms.newts.persistence.cassandra.Utils.getTestCase;

import javax.xml.bind.JAXBException;

import org.junit.Ignore;
import org.junit.Test;


public class PrimaryDataTest {

    private PrimaryData getIterator(XMLTestCase testCase) {
        return new PrimaryData(
                testCase.getResource(),
                testCase.getMetrics(),
                testCase.getStart(),
                testCase.getEnd(),
                testCase.getInterval(),
                getHeartbeats(testCase),
                testCase.getSamples().iterator());
    }

    private void execute(XMLTestCase testCase) {
        assertRowsEqual(testCase.getMeasurements(), getIterator(testCase));
    }

    @Test
    public void testShortSamples() throws JAXBException {
        execute(getTestCase("primaryData/shortSamples.xml"));
    }

    @Test
    public void testSkippedSample() throws JAXBException {
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
