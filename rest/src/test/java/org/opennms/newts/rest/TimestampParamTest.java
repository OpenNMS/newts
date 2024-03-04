/*
 * Copyright 2014-2024, The OpenNMS Group
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
package org.opennms.newts.rest;


import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import javax.ws.rs.WebApplicationException;

import org.junit.Test;
import org.opennms.newts.api.Timestamp;


public class TimestampParamTest {

    @Test(expected = WebApplicationException.class)
    public void testBadSeconds() {
        param("5000000000");
    }

    @Test(expected = WebApplicationException.class)
    public void testBadISO8601() {
        param("1969-12-31 18:00:00-0600");
    }

    @Test
    public void test() {

        assertThat(param("0"), is(Timestamp.fromEpochMillis(0)));
        assertThat(param("900000000"), is(Timestamp.fromEpochSeconds(900000000)));
        assertThat(param("1998-07-09T11:00:00-0500"), is(Timestamp.fromEpochSeconds(900000000)));
        assertThat(param("1969-12-31T18:00:00-0600"), is(Timestamp.fromEpochMillis(0)));

    }

    private Timestamp param(String input) {
        return new TimestampParam(input).get();
    }

}
