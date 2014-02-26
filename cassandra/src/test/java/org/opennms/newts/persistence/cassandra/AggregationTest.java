package org.opennms.newts.persistence.cassandra;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.opennms.newts.api.query.Datasource.StandardAggregationFunctions.AVERAGE;

import java.util.Iterator;

import org.junit.Test;
import org.opennms.newts.api.Duration;
import org.opennms.newts.api.Measurement;
import org.opennms.newts.api.Results;
import org.opennms.newts.api.Results.Row;
import org.opennms.newts.api.Timestamp;
import org.opennms.newts.api.query.ResultDescriptor;


public class AggregationTest {

    @Test
    public void test() {

        Results<Measurement> in = new Results<>();
        Timestamp start = Timestamp.fromEpochSeconds(1);
        String resource = "localhost";
        Duration interval = Duration.seconds(300);

        in.addElement(new Measurement(start, resource, "m0", 1));
        in.addElement(new Measurement(start.plus(interval.times(1)), resource, "m0", 1));
        in.addElement(new Measurement(start.plus(interval.times(2)), resource, "m0", 1));
        in.addElement(new Measurement(start.plus(interval.times(3)), resource, "m0", 1));
        in.addElement(new Measurement(start.plus(interval.times(4)), resource, "m0", 1));
        in.addElement(new Measurement(start.plus(interval.times(5)), resource, "m0", 1));
        in.addElement(new Measurement(start.plus(interval.times(6)), resource, "m0", 3));
        in.addElement(new Measurement(start.plus(interval.times(7)), resource, "m0", 3));
        in.addElement(new Measurement(start.plus(interval.times(8)), resource, "m0", 3));
        in.addElement(new Measurement(start.plus(interval.times(9)), resource, "m0", 3));
        in.addElement(new Measurement(start.plus(interval.times(10)), resource, "m0", 3));
        in.addElement(new Measurement(start.plus(interval.times(11)), resource, "m0", 3));

        ResultDescriptor resultDescriptor = new ResultDescriptor(interval).datasource("m0-avg", "m0", AVERAGE);
        Aggregation agg = new Aggregation(
                resultDescriptor,
                resource,
                start,
                start.plus(interval.times(11)),
                Duration.minutes(60),
                in.iterator());

        Iterator<Row<Measurement>> iter = agg.iterator();

        assertTrue(iter.hasNext());
        assertEquals(Double.NaN, iter.next().getElement("m0-avg").getValue(), 0.0d);
        assertTrue(iter.hasNext());
        assertEquals(2.0d, iter.next().getElement("m0-avg").getValue(), 0.0d);

    }

}
