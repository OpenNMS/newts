package org.opennms.newts.persistence.cassandra;


import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.opennms.newts.api.Duration;
import org.opennms.newts.api.Timestamp;

import com.google.common.collect.Lists;


public class IntervalGeneratorTest {

    private static final Duration DEFAULT_INTERVAL = Duration.seconds(300);

    @Test
    public void test() {

        List<Timestamp> timestamps = Lists.newArrayList(getTimestamps(150, 3500));

        assertEquals(13, timestamps.size());
        assertEquals(new Timestamp(0, TimeUnit.SECONDS), timestamps.get(0));
        assertEquals(new Timestamp(3300, TimeUnit.SECONDS), timestamps.get(11));
        assertEquals(new Timestamp(3600, TimeUnit.SECONDS), timestamps.get(12));

        timestamps = Lists.newArrayList(getTimestamps(0, 3600));

        assertEquals(14, timestamps.size());
        assertEquals(new Timestamp(0, TimeUnit.SECONDS), timestamps.get(0));
        assertEquals(new Timestamp(3600, TimeUnit.SECONDS), timestamps.get(12));
        assertEquals(new Timestamp(3900, TimeUnit.SECONDS), timestamps.get(13));

    }

    private Iterable<Timestamp> getTimestamps(long startSecs, long endSecs) {
        return getTimestamps(startSecs, endSecs, DEFAULT_INTERVAL);
    }

    private Iterable<Timestamp> getTimestamps(long startSecs, long endSecs, Duration duration) {
        return new IntervalGenerator(Timestamp.fromEpochSeconds(startSecs), Timestamp.fromEpochSeconds(endSecs), duration);
    }

}
