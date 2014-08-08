package org.opennms.newts.api;


public class Context {

    public static final Context DEFAULT_CONTEXT = new Context("D");

    private final String m_id;

    public Context(String id) {
        m_id = id;
    }

    public String getId() {
        return m_id;
    }

    @Override
    public String toString() {
        return "Context[m_id=" + m_id + "]";
    }

}
