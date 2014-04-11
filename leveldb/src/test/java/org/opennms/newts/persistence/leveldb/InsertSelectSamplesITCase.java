package org.opennms.newts.persistence.leveldb;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.opennms.newts.api.MetricType.GAUGE;

import java.util.Iterator;
import java.util.List;

import org.junit.Test;
import org.opennms.newts.api.Gauge;
import org.opennms.newts.api.Sample;
import org.opennms.newts.api.Results.Row;
import org.opennms.newts.api.Timestamp;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;


public class InsertSelectSamplesITCase extends AbstractLeveldbTestCase {
    

    @Test
    public void testSamples() {
        int rows = 10, cols = 3, resources=3;
        String resourcePrefix = "r";

        for (int r = 1; r <= resources; r++) {
            List<Sample> samples = Lists.newArrayList();
            for (int i = 1; i <= rows; i++) {
                Timestamp ts = Timestamp.fromEpochMillis(i * 1000);

                for (int j = 1; j <= cols; j++) {
                    Sample s = new Sample(ts, resourcePrefix+r, "m" + j, GAUGE, new Gauge((i + 1) * j));
                    System.err.printf("Saving Sample: %s\n", s);
                    samples.add(s);
                }
            }

            getRepository().insert(samples);
        }
        String resource = resourcePrefix+"2";
        Timestamp start = Timestamp.fromEpochMillis(0), end = Timestamp.fromEpochMillis(rows * 1000);
        Iterator<Row<Sample>> results = getRepository().select(resource, Optional.of(start), Optional.of(end)).iterator();

        for (int i = 1; i <= rows; i++) {
            assertTrue("Insufficient number of results", results.hasNext());

            Timestamp timestamp = Timestamp.fromEpochMillis(i * 1000);
            Row<Sample> row = results.next();

            assertEquals("Unexpected timestamp for row " + i, timestamp, row.getTimestamp());
            assertEquals("Unexpected resource name", resource, row.getResource());

            for (int j = 1; j <= cols; j++) {
                assertNotNull("Missing sample: m" + j, row.getElement("m" + j));

                Sample sample = row.getElement("m" + j);
                
                System.err.printf("Retrieved Sample: %s\n", sample);

                assertEquals("Unexpected timestamp for metric m" + j, timestamp, sample.getTimestamp());
                assertEquals("Unexpected resource name", resource, sample.getResource());
                assertEquals("Unexpected metric name", "m" + j, sample.getName());
                assertEquals("Unexpected metric type", GAUGE, sample.getType());
                assertEquals((double) ((i + 1) * j), sample.getValue().doubleValue(), 0.0d);
            }

        }

    }
    
    @Test
    public void testSize( ) {
        int rows = 12*24*1, cols = 10, resources=100000;
        String resourcePrefix = "r";
        
        
        int count = 0;
        int total = rows*resources*cols;
        long start = System.currentTimeMillis();
        int samplesPerRow = resources*cols;
        for (int i = 1; i <= rows; i++) {
            Timestamp ts = Timestamp.fromEpochMillis(i * 1000);
            List<Sample> samples = Lists.newArrayList();

            long startRow = System.currentTimeMillis();
            for (int r = 1; r <= resources; r++) {
                String res = resourcePrefix + r;
                for (int j = 1; j <= cols; j++) {
                    Sample s = new Sample(ts, res, "m" + j, GAUGE, new Gauge((i + 1) * j));
                    samples.add(s);
                    count++;
                }
            }

            getRepository().insert(samples);
            long now = System.currentTimeMillis();
            double elapsed = (now-start)/1000.0;
            double elapsedRow = (now-startRow)/1000.0;
            System.err.printf("Finished row %d of %d (%d samples): sample/s: %f (overall: %f) seconds/row: %f \n", i, rows, count, samplesPerRow/elapsedRow, count/elapsed, elapsed/i);
        }
        
        long now = System.currentTimeMillis();
        double elapsed = (now-start)/1000.0;
        System.err.printf("Finished %d rows (%d samples): totalElapseTime seconds: %f samples/s: %f seconds/row: %f \n", rows, count, elapsed, count/elapsed, elapsed/rows);

        
    }

}
