package org.opennms.newts.rest;


import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

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
