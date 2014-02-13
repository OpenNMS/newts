package org.opennms.newts.api.query;


import static com.google.common.base.Preconditions.checkNotNull;


public class Aggregate {

    public enum Function {
        AVERAGE, MAXIMUM, MINIMUM;
    }

    private final Function m_function;
    private final String m_label;
    private final String m_source;

    public Aggregate(Function func, String label, String source) {
        checkNotNull(func, "function argument");
        checkNotNull(label, "label argument");
        checkNotNull(source, "source argument");

        m_function = func;
        m_label = label;
        m_source = source;
    }

    public Function getFunction() {
        return m_function;
    }

    public String getLabel() {
        return m_label;
    }

    public String getSource() {
        return m_source;
    }

}
