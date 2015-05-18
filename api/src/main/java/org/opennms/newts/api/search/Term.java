/*
 * Copyright 2014, The OpenNMS Group
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
package org.opennms.newts.api.search;

import static com.google.common.base.Preconditions.checkNotNull;

public class Term {

    private final String m_field;
    private final String m_value;

    /**
     * If field is null, the implementation will use the default field.
     */
    public Term(String field, String value) {
        m_field = field;
        m_value = checkNotNull(value, "value argument");
    }

    /**
     * Use the default field.
     */
    public Term(String value) {
        this(null, value);
    }

    public String getField(String defaultField) {
        return m_field == null ? defaultField : m_field;
    }

    public String getValue() {
        return m_value;
    }

    @Override
    public boolean equals(Object obj) {
       if (obj == null) {
          return false;
       }
       if (getClass() != obj.getClass()) {
          return false;
       }
       final Term other = (Term) obj;

       return   com.google.common.base.Objects.equal(m_field, other.m_field)
             && com.google.common.base.Objects.equal(m_value, other.m_value);
    }

    @Override
    public int hashCode() {
       return com.google.common.base.Objects.hashCode(
               m_field, m_value);
    }

    @Override
    public String toString() {
        return m_field == null ? escapeChars(m_value) : String.format("%s:%s",
                escapeChars(m_field), escapeChars(m_value));
    }

    private static String escapeChars(String token) {
        // Escape any backslashes before we add more
        token = token.replace("\\", "\\\\");
        // Escape colons
        token = token.replace(":", "\\:");
        return token;
    }
}
