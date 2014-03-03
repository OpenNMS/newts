package org.opennms.newts.persistence.cassandra;


import static org.opennms.newts.persistence.cassandra.Utils.getResultDescriptor;

import org.junit.Test;


public class AggregationTest extends AbstractXMLTestCase {

    Aggregation getIterator(XMLTestSpecification testCase) {
        return new Aggregation(
                testCase.getResource(),
                testCase.getStart(),
                testCase.getEnd(),
                getResultDescriptor(testCase),
                testCase.getResolution(),
                testCase.getTestDataAsMeasurements().iterator());
    }

    @Test
    public void testMultipleAggregations() {
        execute("aggregation/multiple_aggregations.xml");
    }

    @Test
    public void testOneHour() {
        execute("aggregation/one_hour.xml");
    }

}
