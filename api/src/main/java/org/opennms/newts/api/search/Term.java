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
