/*
 * Copyright 2014, The OpenNMS Group
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.opennms.newts.aggregate;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.opennms.newts.api.MetricType.COUNTER;
import static org.opennms.newts.api.MetricType.GAUGE;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.opennms.newts.aggregate.Rate;
import org.opennms.newts.aggregate.Utils.SampleRowsBuilder;
import org.opennms.newts.api.Counter;
import org.opennms.newts.api.Duration;
import org.opennms.newts.api.Gauge;
import org.opennms.newts.api.MetricType;
import org.opennms.newts.api.Resource;
import org.opennms.newts.api.Results;
import org.opennms.newts.api.Sample;
import org.opennms.newts.api.Timestamp;
import org.opennms.newts.api.Results.Row;

import com.google.common.collect.Sets;


public class RateTest {

    private static Resource m_resource = new Resource("localhost");
    private static String[] m_metrics = new String[25];

    static {
        for (int i = 0; i < m_metrics.length; i++) {
            m_metrics[i] = String.format("bytes.%d", (i + 1));
        }
    }

    @Before
    public void setUp() {
        Counter.NAN_ON_COUNTER_WRAP = false;
    }

    @Test
    public void testMissing() {

        Results<Sample> input = new Results<>();
        Timestamp start = Timestamp.fromEpochMillis(1000);
        Duration step = Duration.seconds(1);

        // row_1
        input.addElement(new Sample(start, m_resource, m_metrics[0], COUNTER, new Counter(0)));
        input.addElement(new Sample(start, m_resource, m_metrics[1], COUNTER, new Counter(0)));

        // row_2
        input.addElement(new Sample(start.plus(step), m_resource, m_metrics[0], COUNTER, new Counter(100)));
        input.addElement(new Sample(start.plus(step), m_resource, m_metrics[1], COUNTER, new Counter(100)));

        // row_3 (sample for m_metrics[0] missing)
        input.addElement(new Sample(start.plus(step.times(2)), m_resource, m_metrics[1], COUNTER, new Counter(200)));

        // row_4
        input.addElement(new Sample(start.plus(step.times(3)), m_resource, m_metrics[0], COUNTER, new Counter(300)));
        input.addElement(new Sample(start.plus(step.times(3)), m_resource, m_metrics[1], COUNTER, new Counter(300)));

        Iterator<Results.Row<Sample>> output = new Rate(input.iterator(), getMetrics(2)).iterator();

        // result_1 is always null
        assertTrue(output.hasNext());
        assertEquals(new Gauge(Double.NaN), output.next().getElement(m_metrics[0]).getValue());

        // result_2, rate 100
        assertTrue(output.hasNext());
        assertEquals(100.0d, output.next().getElement(m_metrics[0]).getValue().doubleValue(), 0.0d);

        // result_3, missing because sample in row_3 is missing
        assertTrue(output.hasNext());
        assertNull(output.next().getElement(m_metrics[0]));

        // result_4, rate of 100 calculated between row_4 and row_2 
        assertTrue(output.hasNext());
        assertEquals(100.0d, output.next().getElement(m_metrics[0]).getValue().doubleValue(), 0.0d);

    }

    @Test
    public void test() {

        Results<Sample> input = new Results<>();
        int rows = 10, cols = 2, rate = 100;

        for (int i = 1; i <= rows; i++) {
            Timestamp t = Timestamp.fromEpochMillis(i * 1000);

            for (int j = 0; j < cols; j++) {
                input.addElement(new Sample(t, m_resource, m_metrics[j], COUNTER, new Counter((i + j) * rate)));
            }
        }

        Iterator<Results.Row<Sample>> output = new Rate(input.iterator(), getMetrics(cols)).iterator();

        for (int i = 1; i <= rows; i++) {
            assertTrue("Insufficient number of results", output.hasNext());

            Results.Row<Sample> row = output.next();

            assertEquals("Unexpected row timestamp", Timestamp.fromEpochMillis(i * 1000), row.getTimestamp());
            assertEquals("Unexpected row resource", m_resource, row.getResource());
            assertEquals("Unexpected number of columns", cols, row.getElements().size());

            for (int j = 0; j < cols; j++) {
                String name = m_metrics[j];

                assertNotNull("Missing sample" + name, row.getElement(name));
                assertEquals("Unexpected sample name", name, row.getElement(name).getName());
                assertEquals("Unexpected sample type", GAUGE, row.getElement(name).getType());

                // Samples in the first row are null, this is normal.
                if (i != 1) {
                    assertEquals("Incorrect rate value", 100.0d, row.getElement(name).getValue().doubleValue(), 0.0d);
                }
            }
        }

    }

    /**
     * Verifies that counters (unsigned long) values return doubles with proper decimals
     * when converted to rates.
     */
    @Test
    public void ratesWithDecimals() {
        Iterator<Row<Sample>> samples = new SampleRowsBuilder(new Resource("localhost"), MetricType.COUNTER)
                .row(1414598400).element("m1", 9223372034564703200.00)
                .row(1414602000).element("m1", 9223372034601613300.00)
                .row(1414605600).element("m1", 9223372034604530700.00)
                .row(1414609200).element("m1", 9223372034608910300.00)
                .row(1414612800).element("m1", 9223372034636612600.00)
                .row(1414616400).element("m1", 9223372034639099900.00)
                .row(1414620000).element("m1", 9223372034641185800.00)
                .row(1414623600).element("m1", 9223372034642181100.00)
                .build();

        Iterator<Results.Row<Sample>> output = new Rate(samples, Sets.newHashSet("m1")).iterator();

        double expectedRates[] = new double[] {
                Double.NaN,
                10252.800000,
                810.382222,
                1216.568889,
                7695.075556,
                690.915556,
                579.413333,
                276.480000
        };

        for (int i = 0; i < expectedRates.length; i++) {
            double actualRate = output.next().getElement("m1").getValue().doubleValue();
            assertEquals(expectedRates[i], actualRate, 0.0001);
        }
    }

    @Test
    public void testCounterWrap() {
        Iterator<Row<Sample>> samples = new SampleRowsBuilder(new Resource("localhost"), MetricType.COUNTER)
                .row(1).element("m1", 1)
                .row(2).element("m1", 2)
                .row(3).element("m1", 3)
                .row(4).element("m1", 1)
                .row(5).element("m1", 2)
                .row(6).element("m1", 3)
                .build();

        Iterator<Results.Row<Sample>> output = new Rate(samples, Sets.newHashSet("m1")).iterator();

        double expectedRates[] = new double[] {
                Double.NaN,
                1.0,
                1.0,
                4.294967294E9,
                1.0,
                1.0
        };

        for (int i = 0; i < expectedRates.length; i++) {
            double actualRate = output.next().getElement("m1").getValue().doubleValue();
            assertEquals(expectedRates[i], actualRate, 0.0001);
        }
    }

    @Test
    public void testNanOnCounterWrap() {
        Counter.NAN_ON_COUNTER_WRAP = true;

        Iterator<Row<Sample>> samples = new SampleRowsBuilder(new Resource("localhost"), MetricType.COUNTER)
                .row(1).element("m1", 1)
                .row(2).element("m1", 2)
                .row(3).element("m1", 3)
                .row(4).element("m1", 1)
                .row(5).element("m1", 2)
                .row(6).element("m1", 3)
                .build();

        Iterator<Results.Row<Sample>> output = new Rate(samples, Sets.newHashSet("m1")).iterator();

        double expectedRates[] = new double[] {
                Double.NaN,
                1.0,
                1.0,
                Double.NaN,
                1.0,
                1.0
        };

        for (int i = 0; i < expectedRates.length; i++) {
            double actualRate = output.next().getElement("m1").getValue().doubleValue();
            assertEquals(expectedRates[i], actualRate, 0.0001);
        }
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testRemove() {
	Iterator<Results.Row<Sample>> input = new Rate(new Results<Sample>().iterator(), getMetrics(2)).iterator();
	input.remove();
    }

    private Set<String> getMetrics(int number) {
        return Sets.newHashSet(Arrays.copyOf(m_metrics, number));
    }

}
