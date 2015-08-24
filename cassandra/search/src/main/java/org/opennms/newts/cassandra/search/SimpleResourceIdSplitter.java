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

import java.util.List;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;

/**
 * A simple splitter. Has no support for escaping the separator.
 *
 * @author jwhite
 */
public class SimpleResourceIdSplitter implements ResourceIdSplitter {

    private static Splitter s_pathSplitter = Splitter.on(SEPARATOR).omitEmptyStrings().trimResults();

    @Override
    public List<String> splitIdIntoElements(String id) {
        return s_pathSplitter.splitToList(id);
    }

    @Override
    public String joinElementsToId(List<String> elements) {
        Preconditions.checkNotNull(elements, "elements argument");
        StringBuilder sb = new StringBuilder();
        for (String el : elements) {
            // Skip null elements
            if (el == null) {
                continue;
            }

            // Trim the elements and skip any empty ones
            String trimmedEl = el.trim();
            if (trimmedEl.length() < 1)  {
                continue;
            }

            if (sb.length() > 0) {
                sb.append(SEPARATOR);
            }
            sb.append(trimmedEl);
        }
        return sb.toString();
    }

}
