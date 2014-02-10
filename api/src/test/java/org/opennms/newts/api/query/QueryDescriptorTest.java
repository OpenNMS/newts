package org.opennms.newts.api.query;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.opennms.newts.api.Duration;


public class QueryDescriptorTest {

    @Test
    public void testStep() {
        assertEquals(400000, new QueryDescriptor(400000).getStep().asMillis());
        assertEquals(400000, new QueryDescriptor(Duration.millis(400000)).getStep().asMillis());
        assertEquals(QueryDescriptor.DEFAULT_STEP, new QueryDescriptor().getStep().asMillis());

        assertEquals(400000, new QueryDescriptor().step(400000).getStep().asMillis());
        assertEquals(400000, new QueryDescriptor().step(Duration.millis(400000)).getStep().asMillis());

        assertTrue(new QueryDescriptor().step(1000) instanceof QueryDescriptor);
        assertTrue(new QueryDescriptor().step(Duration.millis(1000)) instanceof QueryDescriptor);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBadExport() {
        new QueryDescriptor().datasource("ifInOctets").average("in", "ifInOctets").export("bogus");
    }

    @Test
    public void testExports() {
        QueryDescriptor descriptor = new QueryDescriptor().datasource("ifInOctets").average("in", "ifInOctets").export("in");

        assertEquals(1, descriptor.getExports().size());
        assertEquals("in", descriptor.getExports().iterator().next());
    }

    @Test
    public void testAverage() {

        QueryDescriptor descriptor;

        descriptor = new QueryDescriptor().datasource("temperature");
        descriptor.average("avgTemp", "temperature");

        Aggregate aggregate = descriptor.getAggregates().get("avgTemp");

        assertEquals(Function.AVERAGE, aggregate.getFunction());
        assertEquals("avgTemp", aggregate.getName());
        assertEquals(1, aggregate.getSources().length);
        assertEquals("temperature", aggregate.getSources()[0]);

    }

    @Test
    public void testMin() {

        QueryDescriptor descriptor;

        descriptor = new QueryDescriptor().datasource("temperature");
        descriptor.min("minTemp", "temperature");

        Aggregate aggregate = descriptor.getAggregates().get("minTemp");

        assertEquals(Function.MINIMUM, aggregate.getFunction());
        assertEquals("minTemp", aggregate.getName());
        assertEquals(1, aggregate.getSources().length);
        assertEquals("temperature", aggregate.getSources()[0]);

    }

    @Test
    public void testMax() {

        QueryDescriptor descriptor;

        descriptor = new QueryDescriptor().datasource("temperature");
        descriptor.max("maxTemp", "temperature");

        Aggregate aggregate = descriptor.getAggregates().get("maxTemp");

        assertEquals(Function.MAXIMUM, aggregate.getFunction());
        assertEquals("maxTemp", aggregate.getName());
        assertEquals(1, aggregate.getSources().length);
        assertEquals("temperature", aggregate.getSources()[0]);

    }

    @Test
    public void testDatasource() {

        QueryDescriptor descriptor;
        Datasource dataSource;

        descriptor = new QueryDescriptor().datasource("ifInOctets");

        dataSource = descriptor.getDatasources().get("ifInOctets");
        assertEquals("ifInOctets", dataSource.getName());
        assertEquals("ifInOctets", dataSource.getSource());

        int heatbeat = QueryDescriptor.DEFAULT_HEARTBEAT_MULTIPLIER * QueryDescriptor.DEFAULT_STEP;
        assertEquals(heatbeat, dataSource.getHeartbeat().asMillis());

        descriptor = new QueryDescriptor();

        dataSource = descriptor.datasource("in", "ifInOctets").getDatasources().get("in");
        assertEquals("in", dataSource.getName());
        assertEquals("ifInOctets", dataSource.getSource());

        descriptor = new QueryDescriptor();

        dataSource = descriptor.datasource("in", "ifInOctets", 900000).getDatasources().get("in");
        assertEquals("in", dataSource.getName());
        assertEquals("ifInOctets", dataSource.getSource());
        assertEquals(900, dataSource.getHeartbeat().asSeconds());

        assertEquals(1, descriptor.getSources().size());
        assertEquals("in", descriptor.getSources().iterator().next());

    }

    @Test
    public void testAggregate() {

        QueryDescriptor descriptor;

        descriptor = new QueryDescriptor().datasource("inBytes", "ifInOctets");
        descriptor.aggregate(new Aggregate(Function.MAXIMUM, "inMax", "inBytes"));

        assertEquals(1, descriptor.getAggregates().size());
        assertTrue(descriptor.getAggregates().containsKey("inMax"));

        assertEquals(2, descriptor.getSources().size());
        assertEquals("inMax", descriptor.getSources().iterator().next());

    }

    @Test(expected = IllegalArgumentException.class)
    public void testAverageWithBadSource() {
        new QueryDescriptor().average("avg", "notreal");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAggregateWithBadSource() {
        new QueryDescriptor().aggregate(new Aggregate(Function.AVERAGE, "average", "bogus", "alsobogus"));
    }

}
