package org.opennms.newts.api.query;


import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;


public class Aggregate {

    private final Function m_function;
    private final String m_name;
    private final String[] m_sources;

    public Aggregate(Function func, String name, String... sources) {
        checkNotNull(func, "function argument");
        checkNotNull(name, "name argument");
        checkArgument(sources.length > 0, "No source specified");

        m_function = func;
        m_name = name;
        m_sources = sources;
    }

    public Function getFunction() {
        return m_function;
    }

    public String getName() {
        return m_name;
    }

    public String[] getSources() {
        return m_sources;
    }

}
