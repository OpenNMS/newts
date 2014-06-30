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
import static org.opennms.newts.api.Timestamp.fromEpochSeconds;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.opennms.newts.aggregate.IntervalGenerator;
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

        assertEquals(13, timestamps.size());
        assertEquals(new Timestamp(0, TimeUnit.SECONDS), timestamps.get(0));
        assertEquals(new Timestamp(3600, TimeUnit.SECONDS), timestamps.get(12));

    }

    @Test
    public void testReversed() {

        List<Timestamp> timestamps = Lists.newArrayList(getTimestamps(150, 3500, true));

        assertEquals(13, timestamps.size());
        assertEquals(new Timestamp(0, TimeUnit.SECONDS), timestamps.get(12));
        assertEquals(new Timestamp(3300, TimeUnit.SECONDS), timestamps.get(1));
        assertEquals(new Timestamp(3600, TimeUnit.SECONDS), timestamps.get(0));

        timestamps = Lists.newArrayList(getTimestamps(0, 3600, true));

        assertEquals(13, timestamps.size());
        assertEquals(new Timestamp(0, TimeUnit.SECONDS), timestamps.get(12));
        assertEquals(new Timestamp(3600, TimeUnit.SECONDS), timestamps.get(0));

    }

    private Iterable<Timestamp> getTimestamps(long startSecs, long endSecs) {
        return getTimestamps(startSecs, endSecs, DEFAULT_INTERVAL, false);
    }

    private Iterable<Timestamp> getTimestamps(long startSecs, long endSecs, boolean reversed) {
        return getTimestamps(startSecs, endSecs, DEFAULT_INTERVAL, reversed);
    }

    private Iterable<Timestamp> getTimestamps(long startSecs, long endSecs, Duration duration, boolean reversed) {
        return new IntervalGenerator(
                fromEpochSeconds(startSecs).stepFloor(duration),
                fromEpochSeconds(endSecs).stepCeiling(duration),
                duration,
                reversed);
    }

}
