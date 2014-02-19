package org.opennms.newts.persistence.cassandra;


import static org.opennms.newts.persistence.cassandra.Utils.assertRowsEqual;
import static org.opennms.newts.persistence.cassandra.Utils.getHeartbeats;
import static org.opennms.newts.persistence.cassandra.Utils.getTestCase;

import javax.xml.bind.JAXBException;

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
                testCase.getMeasurements().iterator());
    }

    @Test
    public void testShortSamples() throws JAXBException {

        XMLTestCase testCase = getTestCase("primaryData/shortSamples.xml");

        assertRowsEqual(testCase.getExpectedResults(), getIterator(testCase));

    }

    @Test
    public void testOneToOneSamples() {

        XMLTestCase testCase = getTestCase("primaryData/oneToOne.xml");

        assertRowsEqual(testCase.getExpectedResults(), getIterator(testCase));

    }

}
