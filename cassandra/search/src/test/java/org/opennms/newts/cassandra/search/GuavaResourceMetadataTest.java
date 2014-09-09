package org.opennms.newts.cassandra.search;


import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.opennms.newts.api.Context;
import org.opennms.newts.api.Resource;


public class GuavaResourceMetadataTest {

    @Test
    public void test() {

        Context c = new Context("c");
        Resource r = new Resource("r");

        ResourceMetadataCache cache = new GuavaResourceMetadataCache(10000);

        assertThat(cache.get(c, r).isPresent(), not(true));

        cache.merge(c, r, new ResourceMetadata());

        assertThat(cache.get(c, r).isPresent(), is(true));
        assertThat(cache.get(c, r).get().containsMetric("m0"), not(true));
        assertThat(cache.get(c, r).get().containsMetric("m1"), not(true));

        cache.merge(c, r, new ResourceMetadata().putMetric("m0").putMetric("m1"));

        assertThat(cache.get(c, r).get().containsMetric("m0"), is(true));
        assertThat(cache.get(c, r).get().containsMetric("m1"), is(true));
        assertThat(cache.get(c, r).get().containsAttribute("meat", "beef"), not(true));
        assertThat(cache.get(c, r).get().containsAttribute("pudding", "bread"), not(true));

        cache.merge(c, r, new ResourceMetadata().putAttribute("meat", "beef"));
        cache.merge(c, r, new ResourceMetadata().putAttribute("pudding", "bread"));

        assertThat(cache.get(c, r).get().containsAttribute("meat", "beef"), is(true));
        assertThat(cache.get(c, r).get().containsAttribute("pudding", "bread"), is(true));

    }

}
