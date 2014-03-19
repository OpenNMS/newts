package org.opennms.newts.rest;


import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import javax.ws.rs.WebApplicationException;

import org.junit.Test;
import org.opennms.newts.api.Duration;


public class DurationParamTest {

    private static final Duration MINUTE = Duration.seconds(60);
    private static final Duration HOUR = MINUTE.times(60);
    private static final Duration DAY = HOUR.times(24);
    private static final Duration WEEK = DAY.times(7);

    @Test(expected = WebApplicationException.class)
    public void testBadSeconds() {
        param("5000000000");
    }

    @Test(expected = WebApplicationException.class)
    public void testBadPeriodSpecifier() {
        param("1nanosecond");
    }

    @Test
    public void test() {

        assertThat(param("60"), is(MINUTE));
        assertThat(param("60s"), is(MINUTE));
        assertThat(param("1m"), is(MINUTE));
        assertThat(param("3600"), is(HOUR));
        assertThat(param("1h"), is(HOUR));
        assertThat(param("1d"), is(DAY));
        assertThat(param("1w"), is(WEEK));

        // Combination
        assertThat(param("1d2h30m"), is(DAY.plus(HOUR.times(2).plus(MINUTE.times(30)))));
        assertThat(param("1w3d8h55m"), is(WEEK.plus(DAY.times(3)).plus(HOUR.times(8).plus(MINUTE.times(55)))));

    }

    private Duration param(String input) {
        return new DurationParam(input).get();
    }

}
