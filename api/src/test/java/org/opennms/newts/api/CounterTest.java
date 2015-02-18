package org.opennms.newts.api;


import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.google.common.primitives.UnsignedLong;


public class CounterTest {

    @Test
    public void testDelta() {

        Counter c0 = new Counter(UnsignedLong.fromLongBits(0xFFFFFFFEL));
        Counter c1 = new Counter(UnsignedLong.fromLongBits(0xFFFFFFFFL));

        assertThat((Counter) c1.delta(c0), is(new Counter(1L)));

    }

    @Test
    public void testDeltaWrap64() {

        Counter c0 = new Counter(UnsignedLong.fromLongBits(0xFFFFFFFFFFFFFFFAL));
        Counter c1 = new Counter(4L);

        assertThat((Counter) c1.delta(c0), is(new Counter(10L)));

    }

    @Test
    public void testDeltaWrap32() {

        Counter c0 = new Counter(UnsignedLong.fromLongBits(0xFFFFFFFAL));
        Counter c1 = new Counter(4L);

        assertThat((Counter) c1.delta(c0), is(new Counter(10L)));

    }

}
