package org.opennms.newts.api.query;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.opennms.newts.api.Duration;


public class ResultDescriptorTest {

    @Test
    public void testStep() {
        assertEquals(400000, new ResultDescriptor(400000).getStep().asMillis());
        assertEquals(400000, new ResultDescriptor(Duration.millis(400000)).getStep().asMillis());
        assertEquals(ResultDescriptor.DEFAULT_STEP, new ResultDescriptor().getStep().asMillis());

        assertEquals(400000, new ResultDescriptor().step(400000).getStep().asMillis());
        assertEquals(400000, new ResultDescriptor().step(Duration.millis(400000)).getStep().asMillis());

        assertTrue(new ResultDescriptor().step(1000) instanceof ResultDescriptor);
        assertTrue(new ResultDescriptor().step(Duration.millis(1000)) instanceof ResultDescriptor);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBadExport() {
        new ResultDescriptor().datasource("ifInOctets").average("in", "ifInOctets").export("bogus");
    }

    @Test
    public void testExports() {
        ResultDescriptor descriptor = new ResultDescriptor().datasource("ifInOctets").average("in", "ifInOctets").export("in");

        assertEquals(1, descriptor.getExports().size());
        assertEquals("in", descriptor.getExports().iterator().next());
    }

    @Test
    public void testAverage() {

        ResultDescriptor descriptor;

        descriptor = new ResultDescriptor().datasource("temperature");
        descriptor.average("avgTemp", "temperature");

        Aggregate aggregate = descriptor.getAggregates().get("avgTemp");

        assertEquals(Function.AVERAGE, aggregate.getFunction());
        assertEquals("avgTemp", aggregate.getName());
        assertEquals(1, aggregate.getSources().length);
        assertEquals("temperature", aggregate.getSources()[0]);

    }

    @Test
    public void testMin() {

        ResultDescriptor descriptor;

        descriptor = new ResultDescriptor().datasource("temperature");
        descriptor.min("minTemp", "temperature");

        Aggregate aggregate = descriptor.getAggregates().get("minTemp");

        assertEquals(Function.MINIMUM, aggregate.getFunction());
        assertEquals("minTemp", aggregate.getName());
        assertEquals(1, aggregate.getSources().length);
        assertEquals("temperature", aggregate.getSources()[0]);

    }

    @Test
    public void testMax() {

        ResultDescriptor descriptor;

        descriptor = new ResultDescriptor().datasource("temperature");
        descriptor.max("maxTemp", "temperature");

        Aggregate aggregate = descriptor.getAggregates().get("maxTemp");

        assertEquals(Function.MAXIMUM, aggregate.getFunction());
        assertEquals("maxTemp", aggregate.getName());
        assertEquals(1, aggregate.getSources().length);
        assertEquals("temperature", aggregate.getSources()[0]);

    }

    @Test
    public void testDatasource() {

        ResultDescriptor descriptor;
        Datasource dataSource;

        descriptor = new ResultDescriptor().datasource("ifInOctets");

        dataSource = descriptor.getDatasources().get("ifInOctets");
        assertEquals("ifInOctets", dataSource.getName());
        assertEquals("ifInOctets", dataSource.getSource());

        int heatbeat = ResultDescriptor.DEFAULT_HEARTBEAT_MULTIPLIER * ResultDescriptor.DEFAULT_STEP;
        assertEquals(heatbeat, dataSource.getHeartbeat().asMillis());

        descriptor = new ResultDescriptor();

        dataSource = descriptor.datasource("in", "ifInOctets").getDatasources().get("in");
        assertEquals("in", dataSource.getName());
        assertEquals("ifInOctets", dataSource.getSource());

        descriptor = new ResultDescriptor();

        dataSource = descriptor.datasource("in", "ifInOctets", 900000).getDatasources().get("in");
        assertEquals("in", dataSource.getName());
        assertEquals("ifInOctets", dataSource.getSource());
        assertEquals(900, dataSource.getHeartbeat().asSeconds());

        assertEquals(1, descriptor.getSources().size());
        assertEquals("in", descriptor.getSources().iterator().next());

    }

    @Test
    public void testAggregate() {

        ResultDescriptor descriptor;

        descriptor = new ResultDescriptor().datasource("inBytes", "ifInOctets");
        descriptor.aggregate(new Aggregate(Function.MAXIMUM, "inMax", "inBytes"));

        assertEquals(1, descriptor.getAggregates().size());
        assertTrue(descriptor.getAggregates().containsKey("inMax"));

        assertEquals(2, descriptor.getSources().size());
        assertEquals("inMax", descriptor.getSources().iterator().next());

    }

    @Test(expected = IllegalArgumentException.class)
    public void testAverageWithBadSource() {
        new ResultDescriptor().average("avg", "notreal");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAggregateWithBadSource() {
        new ResultDescriptor().aggregate(new Aggregate(Function.AVERAGE, "average", "bogus", "alsobogus"));
    }

}
