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
