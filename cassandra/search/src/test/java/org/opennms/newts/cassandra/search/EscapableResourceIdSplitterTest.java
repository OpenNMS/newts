/*
 * Copyright 2015, The OpenNMS Group
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
package org.opennms.newts.cassandra.search;

import org.junit.Test;

import com.google.common.collect.Lists;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.List;

public class EscapableResourceIdSplitterTest {

    private EscapableResourceIdSplitter splitter = new EscapableResourceIdSplitter();

    @Test
    public void canEscapeSeparator() {
        String id = "a\\:a\\:a:\\\\b\\\\:c\\\\";
        List<String> elements = Lists.newArrayList("a:a:a", "\\b\\", "c\\");
        assertThat(splitter.splitIdIntoElements(id), is(elements));
        assertThat(splitter.joinElementsToId(elements), is(id));
    }

    @Test
    public void canSplitIdIntoElements() {
        List<String> els = Lists.newArrayList("a", "b");
        assertThat(splitter.splitIdIntoElements("a:b"), is(els));
        // Empty elements should be omitted
        assertThat(splitter.splitIdIntoElements("a::b"), is(els));
        // Spaces should be trimmed
        assertThat(splitter.splitIdIntoElements(" a : : b "), is(els));
        // Single element
        els = Lists.newArrayList("a");
        assertThat(splitter.splitIdIntoElements("a"), is(els));
        // Empty string
        els = Lists.newArrayList();
        assertThat(splitter.splitIdIntoElements(""), is(els));
    }

    @Test
    public void canJoinElementsToId() {
        assertThat(splitter.joinElementsToId(Lists.newArrayList("a", "b", "c")), is("a:b:c"));
        // Empty elements should be omitted
        assertThat(splitter.joinElementsToId(Lists.newArrayList("a", "", "b")), is("a:b"));
        // null elements should be omitted
        assertThat(splitter.joinElementsToId(Lists.newArrayList("a", null, "b")), is("a:b"));
        // Spaces should be trimmed
        assertThat(splitter.joinElementsToId(Lists.newArrayList(" a ", " b ", " ")), is("a:b"));
        // Single element
        assertThat(splitter.joinElementsToId(Lists.newArrayList("a")), is("a"));
        // Empty string
        assertThat(splitter.joinElementsToId(Lists.newArrayList("")), is(""));
    }

}
