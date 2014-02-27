package org.opennms.newts.persistence.cassandra;


import org.junit.Test;
import org.opennms.newts.api.query.ResultDescriptor;


public class AggregationTest extends AbstractXMLTestCase {

    Aggregation getIterator(XMLTestSpecification testCase) {
        ResultDescriptor resultDescriptor = new ResultDescriptor(testCase.getInterval());

        for (XMLDatasource ds : testCase.getDatasources()) {
            resultDescriptor.datasource(ds.getLabel(), ds.getSource(), ds.getFunction());
        }

        return new Aggregation(
                resultDescriptor,
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
