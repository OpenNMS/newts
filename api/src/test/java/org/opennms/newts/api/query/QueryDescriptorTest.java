package org.opennms.newts.api.query;


import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opennms.newts.api.Duration;
import org.opennms.newts.api.query.QueryDescriptor;


public class QueryDescriptorTest {

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testStep() {
        assertEquals(400000, new QueryDescriptor(400000).getStep().asMillis());
        assertEquals(400000, new QueryDescriptor(Duration.millis(400000)).getStep().asMillis());
        assertEquals(QueryDescriptor.DEFAULT_STEP, new QueryDescriptor().getStep().asMillis());

        assertEquals(400000, new QueryDescriptor().step(400000).getStep().asMillis());
        assertEquals(400000, new QueryDescriptor().step(Duration.millis(400000)).getStep().asMillis());

        assertTrue(new QueryDescriptor().step(1000) instanceof QueryDescriptor);
        assertTrue(new QueryDescriptor().step(Duration.millis(1000)) instanceof QueryDescriptor);
    }

}
