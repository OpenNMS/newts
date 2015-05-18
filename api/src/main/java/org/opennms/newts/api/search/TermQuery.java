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

public class TermQuery extends Query {

    private final Term m_term;

    public TermQuery(Term term) {
        checkNotNull(term, "term argument");
        m_term = term;
    }

    public Term getTerm() {
        return m_term;
    }

    @Override
    public boolean equals(Object obj) {
       if (obj == null) {
          return false;
       }
       if (getClass() != obj.getClass()) {
          return false;
       }
       final TermQuery other = (TermQuery) obj;

       return com.google.common.base.Objects.equal(m_term, other.m_term);
    }

    @Override
    public int hashCode() {
       return com.google.common.base.Objects.hashCode(
               m_term);
    }

    @Override
    public String toString() {
        return m_term.toString();
    }
}
