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
import com.google.common.collect.Lists;

/**
 * A splitter that supports escaping the separator.
 *
 * @author jwhite
 */
public class EscapableResourceIdSplitter implements ResourceIdSplitter {

    /**
     * Splits a resource id into a list of elements.
     *
     * Elements in the resource id are delimited by colons (:).
     * Colons can be escaped by a backslash (\:).
     * Backslashes are escaped when paired (\\).
     *
     * For example, the following resources id:
     *    a:b\::c\\:d
     * will result in the following elements:
     *    a, b:, c\, d
     *
     * @param id the resource id
     * @return a list of elements
     */
    @Override
    public List<String> splitIdIntoElements(String id) {
        Preconditions.checkNotNull(id, "id argument");
        List<String> elements = Lists.newArrayList();

        int startOfNextElement = 0;
        int numConsecutiveEscapeCharacters = 0;
        char[] idChars = id.toCharArray();
        for (int i = 0; i < idChars.length; i++) {
            // If we hit a separator, only treat it as such if it is preceded by an even number of escape characters
            if(idChars[i] == SEPARATOR && numConsecutiveEscapeCharacters % 2 == 0) {
                maybeAddSanitizedElement(new String(idChars, startOfNextElement, i-startOfNextElement), elements);
                startOfNextElement = i+1;
            }

            if (idChars[i] == '\\') {
                numConsecutiveEscapeCharacters++;
            } else {
                numConsecutiveEscapeCharacters = 0;
            }
        }

        maybeAddSanitizedElement(new String(idChars, startOfNextElement, idChars.length - startOfNextElement), elements);
        return elements;
    }

    /**
     * Maybe adds to element to the list after being sanitized.
     */
    private static void maybeAddSanitizedElement(final String element, final List<String> elements) {
        // Trim the element and skip it when empty
        String sanitizedElement = element.trim();
        if (sanitizedElement.length() == 0) {
            return;
        }
        // \: -> :
        sanitizedElement = sanitizedElement.replaceAll("\\\\:", ":");
        // \\ -> \
        sanitizedElement = sanitizedElement.replaceAll("\\\\\\\\", "\\\\");
        elements.add(sanitizedElement);
    }

    /**
     * Joins a list of elements into a resource id, escaping
     * special characters if required.
     *
     * See {@link #splitIdIntoElements(String)} for details.
     *
     * @param elements a list of elements
     * @return the resource id
     */
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

            // \ -> \\
            trimmedEl = trimmedEl.replaceAll("\\\\", "\\\\\\\\");
            // : -> \:
            trimmedEl = trimmedEl.replaceAll(":", "\\\\:");

            if (sb.length() > 0) {
                sb.append(SEPARATOR);
            }
            sb.append(trimmedEl);
        }
        return sb.toString();
    }
}
