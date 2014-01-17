package org.opennms.newts;


import static org.junit.Assert.assertEquals;

import org.junit.Test;


public class NewtsEndpointTest {

    @Test
    public void testUrlParsing() {
        assertEquals("localhost", getEndpoint("newts://:9042/foo").getHostname());
        assertEquals("localhost", getEndpoint("newts://localhost:9042/foo").getHostname());
        assertEquals("localhost", getEndpoint("newts://localhost/foo").getHostname());
        assertEquals(9042, getEndpoint("newts://:9042/foo").getPort());
        assertEquals(9042, getEndpoint("newts://localhost/foo").getPort());
        assertEquals("foo", getEndpoint("newts://:9042/foo").getKeyspace());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUrlParsingWithInvalidKeyspace() {
        getEndpoint("newts://localhost/100");
    }

    private static NewtsEndpoint getEndpoint(String uri) {
        return new NewtsEndpoint(uri, null);
    }

}
