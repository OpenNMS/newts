package org.opennms.newts.persistence.cassandra;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.opennms.newts.api.MetricType.GAUGE;

import java.util.Iterator;
import java.util.List;

import org.junit.Test;
import org.opennms.newts.api.Gauge;
import org.opennms.newts.api.Measurement;
import org.opennms.newts.api.Results.Row;
import org.opennms.newts.api.Timestamp;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;


public class InsertITCase extends AbstractCassandraTestCase {

    @Test
    public void test() {

        List<Measurement> measurements = Lists.newArrayList();
        int rows = 10, cols = 3;
        String resource = "r";

        for (int i = 1; i <= rows; i++) {
            Timestamp ts = Timestamp.fromEpochMillis(i * 1000);

            for (int j = 1; j <= cols; j++) {
                measurements.add(new Measurement(ts, resource, "m" + j, GAUGE, new Gauge((i + 1) * j)));
            }
        }

        getRepository().insert(measurements);

        Timestamp start = Timestamp.fromEpochMillis(0), end = Timestamp.fromEpochMillis(rows * 1000);
        Iterator<Row> results = getRepository().select(resource, Optional.of(start), Optional.of(end)).iterator();

        for (int i = 1; i <= rows; i++) {
            assertTrue("Insufficient number of results", results.hasNext());

            Timestamp timestamp = Timestamp.fromEpochMillis(i * 1000);
            Row row = results.next();

            assertEquals("Unexpected timestamp for row " + i, timestamp, row.getTimestamp());
            assertEquals("Unexpected resource name", resource, row.getResource());

            for (int j = 1; j <= cols; j++) {
                assertNotNull("Missing measurement: m" + j, row.getMeasurement("m" + j));

                Measurement measurement = row.getMeasurement("m" + j);

                assertEquals("Unexpected timestamp for metric m" + j, timestamp, measurement.getTimestamp());
                assertEquals("Unexpected resource name", resource, measurement.getResource());
                assertEquals("Unexpected metric name", "m" + j, measurement.getName());
                assertEquals("Unexpected metric type", GAUGE, measurement.getType());
                assertEquals((double) ((i + 1) * j), measurement.getValue().doubleValue(), 0.0d);
            }

        }

    }

}
