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
