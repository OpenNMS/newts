package org.opennms.newts.cli.parse;

import static com.google.common.base.Preconditions.checkNotNull;


abstract class Relation {

    static enum Operator {
        EQ, LT, GT;
    }

    protected String m_name;
    protected String m_value;
    protected Operator m_operator;

    Relation(String name, Operator operator, String value) {
        m_name = checkNotNull(name, "name argument");
        m_operator = checkNotNull(operator, "operator argument");
        m_value = checkNotNull(value, "token argument");
    }

}
