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
