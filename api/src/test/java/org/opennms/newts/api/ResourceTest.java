package org.opennms.newts.api;


import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Collections;
import java.util.Map;

import org.junit.Test;

import com.google.common.base.Optional;


public class ResourceTest {

    @Test
    public void test() {

        Resource r0, r1, r2;

        r0 = new Resource(Optional.<Resource> absent(), "aaa");
        r1 = new Resource(Optional.of(r0), "bbb");
        r2 = new Resource(Optional.of(r1), "ccc");

        assertThat(r0, equalTo(new Resource("/aaa")));
        assertThat(r0.getName(), equalTo("aaa"));
        assertThat(r0.getParent().isPresent(), is(false));
        assertThat(r1, equalTo(new Resource("/aaa/bbb")));
        assertThat(r1.getName(), equalTo("bbb"));
        assertThat(r1.getParent().isPresent(), is(true));
        assertThat(r1.getParent().get(), is(r0));
        assertThat(r2, equalTo(new Resource("/aaa/bbb/ccc")));
        assertThat(r2.getName(), equalTo("ccc"));
        assertThat(r2.getParent().isPresent(), is(true));
        assertThat(r2.getParent().get(), is(r1));

    }

    @Test
    public void testParsing() {
        assertThat(new Resource("/aaa/bbb/ccc").getId(), equalTo("/aaa/bbb/ccc"));
        assertThat(new Resource("/aaa/").getId(), equalTo("/aaa"));
        assertThat(new Resource("aaa/bbb/ccc").getId(), equalTo("/aaa/bbb/ccc"));
        assertThat(new Resource("aaa").getId(), equalTo("/aaa"));
        assertThat(new Resource("//aaa//bbb////ccc").getId(), equalTo("/aaa/bbb/ccc"));
    }

    @Test
    public void testAttributes() {

        Map<String, String> attrs = Collections.singletonMap("side", "beans");

        Resource r = new Resource("/sandwich/brisket", attrs);

        assertThat(r.getAttributes().containsKey("side"), is(true));
        assertThat(r.getAttributes().get("side"), is(equalTo("beans")));

        r = new Resource(Optional.of(new Resource(Optional.<Resource> absent(), "sandwich")), "brisket", attrs);

        assertThat(r.getAttributes().containsKey("side"), is(true));
        assertThat(r.getAttributes().get("side"), is(equalTo("beans")));

    }

}
