package org.opennms.newts.stress;


abstract class Worker extends Thread {

    private boolean m_isShutdown = false;

    Worker(String name) {
        super(name);
    }

    public abstract void run();

    boolean isShutdown() {
        return m_isShutdown;
    }

    void shutdown() {
        m_isShutdown = true;
    }

}
