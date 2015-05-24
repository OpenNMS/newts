package org.opennms.newts.graphite;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.opennms.newts.graphite.GraphiteHandler.parseSample;

import org.junit.Test;
import org.opennms.newts.api.Sample;

public class GraphiteHandlerTest {

    @Test
    public void testParseSample() {
        Sample sample = parseSample("foo.bar.baz 5 10000");

        assertThat(sample.getResource().getId(), is("foo:bar"));
        assertThat(sample.getName(), is("baz"));
        assertThat(sample.getValue().intValue(), equalTo(5));
        assertThat(sample.getTimestamp().asSeconds(), equalTo(10000L));

        sample = parseSample("foo 5 10000");
        assertThat(sample.getResource().getId(), is("foo"));
        assertThat(sample.getName(), is("value"));

    }

}
