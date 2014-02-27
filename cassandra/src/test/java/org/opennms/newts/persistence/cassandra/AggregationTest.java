package org.opennms.newts.persistence.cassandra;


import static org.opennms.newts.persistence.cassandra.Utils.getResultDescriptor;

import org.junit.Test;


public class AggregationTest extends AbstractXMLTestCase {

    Aggregation getIterator(XMLTestSpecification testCase) {
        return new Aggregation(
                getResultDescriptor(testCase),
                testCase.getResource(),
                testCase.getStart(),
                testCase.getEnd(),
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
