package org.opennms.newts.api.query;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.opennms.newts.api.Duration.seconds;
import static org.opennms.newts.api.query.Datasource.StandardAggregationFunctions.AVERAGE;
import static org.opennms.newts.api.query.Datasource.StandardAggregationFunctions.MAX;
import static org.opennms.newts.api.query.Datasource.StandardAggregationFunctions.MIN;

import org.junit.Test;
import org.opennms.newts.api.Duration;
import org.opennms.newts.api.query.ResultDescriptor.BinaryFunction;

import com.google.common.collect.Sets;


public class ResultDescriptorTest {
    
    
    
    @Test
    public void testDescriptor() {
        BinaryFunction plus = new BinaryFunction() {
            
            @Override
            public double apply(double a, double b) {
                return a + b;
            }
        };
        BinaryFunction minus = new BinaryFunction() {
            
            @Override
            public double apply(double a, double b) {
                return a - b;
            }
        };
        
        ResultDescriptor results = new ResultDescriptor()
            .datasource("in", "ifInOctets", seconds(600), AVERAGE)
            .datasource("out", "ifOutOctets", seconds(600), AVERAGE)
            .calculate("sum", plus, "in", "out")
            .calculate("diff", minus, "in", "out")
            .sum("total", "in", "out")
            .export("sum", "diff", "total");
        
        assertEquals(Sets.newHashSet("in", "out"), results.getDatasources().keySet());
        
        assertEquals(Sets.newHashSet("sum", "diff", "total"), results.getExports());
    }

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
        new ResultDescriptor().datasource("in", "ifInOctets", AVERAGE).export("bogus");
    }

    @Test
    public void testExports() {
        ResultDescriptor descriptor = new ResultDescriptor().datasource("in", "ifInOctets", AVERAGE).export("in");

        assertEquals(1, descriptor.getExports().size());
        assertEquals("in", descriptor.getExports().iterator().next());
    }

    @Test
    public void testAverage() {

        ResultDescriptor descriptor;

        descriptor = new ResultDescriptor().datasource("avgTemp", "temperature", AVERAGE);

        Datasource ds = descriptor.getDatasources().get("avgTemp");
        assertEquals(AVERAGE, ds.getAggregationFuction());
        assertEquals("avgTemp", ds.getLabel());
        assertEquals("temperature", ds.getSource());

    }

    @Test
    public void testMin() {

        ResultDescriptor descriptor;

        descriptor = new ResultDescriptor().datasource("minTemp", "temperature", MIN);

        Datasource ds = descriptor.getDatasources().get("minTemp");
        assertEquals(MIN, ds.getAggregationFuction());
        assertEquals("minTemp", ds.getLabel());
        assertEquals("temperature", ds.getSource());

    }

    @Test
    public void testMax() {

        ResultDescriptor descriptor;

        descriptor = new ResultDescriptor().datasource("maxTemp", "temperature", MAX);

        Datasource ds = descriptor.getDatasources().get("maxTemp");
        assertEquals(MAX, ds.getAggregationFuction());
        assertEquals("maxTemp", ds.getLabel());
        assertEquals("temperature", ds.getSource());
    }

    @Test
    public void testDatasource() {

        ResultDescriptor descriptor;
        Datasource dataSource;

        descriptor = new ResultDescriptor().datasource("ifInOctets", AVERAGE);

        dataSource = descriptor.getDatasources().get("ifInOctets");
        assertEquals("ifInOctets", dataSource.getLabel());
        assertEquals("ifInOctets", dataSource.getSource());

        int heatbeat = ResultDescriptor.DEFAULT_HEARTBEAT_MULTIPLIER * ResultDescriptor.DEFAULT_STEP;
        assertEquals(heatbeat, dataSource.getHeartbeat().asMillis());

        descriptor = new ResultDescriptor();

        dataSource = descriptor.datasource("in", "ifInOctets", AVERAGE).getDatasources().get("in");
        assertEquals("in", dataSource.getLabel());
        assertEquals("ifInOctets", dataSource.getSource());

        descriptor = new ResultDescriptor();

        dataSource = descriptor.datasource("in", "ifInOctets", 900000, AVERAGE).getDatasources().get("in");
        assertEquals("in", dataSource.getLabel());
        assertEquals("ifInOctets", dataSource.getSource());
        assertEquals(900, dataSource.getHeartbeat().asSeconds());

        assertEquals(1, descriptor.getDatasources().size());
        assertEquals("in", descriptor.getLabels().iterator().next());

    }

}
