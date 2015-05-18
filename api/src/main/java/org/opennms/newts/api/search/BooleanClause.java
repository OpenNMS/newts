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

public class BooleanClause {

    private final Query m_query;
    private final Operator m_operator;

    public BooleanClause(Query query, Operator operator) {
        checkNotNull(query, "query argument");
        checkNotNull(operator, "operator argument");
        m_query = query;
        m_operator = operator;
    }

    public Query getQuery() {
        return m_query;
    }

    public Operator getOperator() {
        return m_operator;
    }

    @Override
    public boolean equals(Object obj) {
       if (obj == null) {
          return false;
       }
       if (getClass() != obj.getClass()) {
          return false;
       }
       final BooleanClause other = (BooleanClause) obj;

       return   com.google.common.base.Objects.equal(m_query, other.m_query)
             && com.google.common.base.Objects.equal(m_operator, other.m_operator);
    }

    @Override
    public int hashCode() {
       return com.google.common.base.Objects.hashCode(
               m_query, m_operator);
    }

    @Override
    public String toString() {
        return m_query.toString();
    }
}
