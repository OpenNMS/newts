package org.opennms.newts.persistence.cassandra;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.opennms.newts.api.MetricType.COUNTER;
import static org.opennms.newts.api.MetricType.GAUGE;

import java.util.Arrays;
import java.util.Iterator;

import org.junit.Test;
import org.opennms.newts.api.Counter;
import org.opennms.newts.api.Duration;
import org.opennms.newts.api.Sample;
import org.opennms.newts.api.Results;
import org.opennms.newts.api.Timestamp;


public class RateTest {

    private static String m_resource = "localhost";
    private static String[] m_metrics = new String[25];

    static {
        for (int i = 0; i < m_metrics.length; i++) {
            m_metrics[i] = String.format("bytes.%d", (i + 1));
        }
    }

    @Test
    public void testMissing() {

        Results input = new Results();
        Timestamp start = Timestamp.fromEpochMillis(1000);
        Duration step = Duration.seconds(1);

        // row_1
        input.addSample(new Sample(start, m_resource, m_metrics[0], COUNTER, new Counter(0)));
        input.addSample(new Sample(start, m_resource, m_metrics[1], COUNTER, new Counter(0)));

        // row_2
        input.addSample(new Sample(start.plus(step), m_resource, m_metrics[0], COUNTER, new Counter(100)));
        input.addSample(new Sample(start.plus(step), m_resource, m_metrics[1], COUNTER, new Counter(100)));

        // row_3 (sample for m_metrics[0] missing)
        input.addSample(new Sample(start.plus(step.times(2)), m_resource, m_metrics[1], COUNTER, new Counter(200)));

        // row_4
        input.addSample(new Sample(start.plus(step.times(3)), m_resource, m_metrics[0], COUNTER, new Counter(300)));
        input.addSample(new Sample(start.plus(step.times(3)), m_resource, m_metrics[1], COUNTER, new Counter(300)));

        Iterator<Results.Row> output = new Rate(input.iterator(), getMetrics(2)).iterator();

        // result_1 is always null
        assertTrue(output.hasNext());
        assertNull(output.next().getSample(m_metrics[0]).getValue());

        // result_2, rate 100
        assertTrue(output.hasNext());
        assertEquals(100.0d, output.next().getSample(m_metrics[0]).getValue().doubleValue(), 0.0d);

        // result_3, missing because sample in row_3 is missing
        assertTrue(output.hasNext());
        assertNull(output.next().getSample(m_metrics[0]));

        // result_4, rate of 100 calculated between row_4 and row_2 
        assertTrue(output.hasNext());
        assertEquals(100.0d, output.next().getSample(m_metrics[0]).getValue().doubleValue(), 0.0d);

    }

    @Test
    public void test() {

        Results input = new Results();
        int rows = 10, cols = 2, rate = 100;

        for (int i = 1; i <= rows; i++) {
            Timestamp t = Timestamp.fromEpochMillis(i * 1000);

            for (int j = 0; j < cols; j++) {
                input.addSample(new Sample(t, m_resource, m_metrics[j], COUNTER, new Counter((i + j) * rate)));
            }
        }

        Iterator<Results.Row> output = new Rate(input.iterator(), getMetrics(cols)).iterator();

        for (int i = 1; i <= rows; i++) {
            assertTrue("Insufficient number of results", output.hasNext());

            Results.Row row = output.next();

            assertEquals("Unexpected row timestamp", Timestamp.fromEpochMillis(i * 1000), row.getTimestamp());
            assertEquals("Unexpected row resource", m_resource, row.getResource());
            assertEquals("Unexpected number of columns", cols, row.getSamples().size());

            for (int j = 0; j < cols; j++) {
                String name = m_metrics[j];

                assertNotNull("Missing sample" + name, row.getSample(name));
                assertEquals("Unexpected sample name", name, row.getSample(name).getName());
                assertEquals("Unexpected sample type", GAUGE, row.getSample(name).getType());

                // Samples in the first row are null, this is normal.
                if (i != 1) {
                    assertEquals("Incorrect rate value", 100.0d, row.getSample(name).getValue().doubleValue(), 0.0d);
                }
            }
        }

    }

    private String[] getMetrics(int number) {
        return Arrays.copyOf(m_metrics, number);
    }

}
