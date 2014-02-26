package org.opennms.newts.persistence.cassandra;


import static org.opennms.newts.persistence.cassandra.Utils.assertRowsEqual;

import org.junit.Test;
import org.opennms.newts.api.query.ResultDescriptor;


public class AggregationTest {

    @Test
    public void testOneHour() {

        XMLTestCase testCase = Utils.getTestCase("aggregation/one_hour.xml");
        ResultDescriptor resultDescriptor = new ResultDescriptor(testCase.getInterval());

        for (XMLDatasource ds : testCase.getDatasources()) {
            resultDescriptor.datasource(ds.getLabel(), ds.getSource(), ds.getFunction());
        }

        Aggregation aggregation = new Aggregation(
                resultDescriptor,
                testCase.getResource(),
                testCase.getStart(),
                testCase.getEnd(),
                testCase.getResolution(),
                testCase.getTestDataAsMeasurements().iterator());

        assertRowsEqual(testCase.getExpected(), aggregation);

    }

}
