package org.opennms.newts.api.query;


import org.opennms.newts.api.Duration;


public class QueryDescriptor {

    /**
     * The default step size in milliseconds.
     */
    public static final int DEFAULT_STEP = 300000;

    /**
     * Multiple of the step size to use as default heartbeat.
     */
    public static final int DEFAULT_HEARTBEAT_MULTIPLIER = 2;

    private Duration m_step;

    /**
     * Constructs a new {@link QueryDescriptor} with the default step size.
     */
    public QueryDescriptor() {
        this(DEFAULT_STEP);
    }

    /**
     * Constructs a new {@link QueryDescriptor} with the given step size.
     * 
     * @param step
     *            duration in milliseconds
     */
    public QueryDescriptor(long step) {
        this(Duration.millis(step));
    }

    /**
     * Constructs a new {@link QueryDescriptor} with the given step size.
     * 
     * @param step
     *            duration as an instance of {@link Duration}
     */
    public QueryDescriptor(Duration step) {
        m_step = step;
    }

    public Duration getStep() {
        return m_step;
    }

    /**
     * Set the step duration.
     * 
     * @param step
     *            duration in milliseconds
     * @return
     */
    public QueryDescriptor step(long step) {
        return step(Duration.millis(step));
    }

    public QueryDescriptor step(Duration step) {
        m_step = step;
        return this;
    }

    public QueryDescriptor datasource(String metricName) {
        return datasource(metricName, metricName);
    }

    public QueryDescriptor datasource(String name, String metricName) {
        return datasource(name, metricName, getStep().times(DEFAULT_HEARTBEAT_MULTIPLIER));
    }

    public QueryDescriptor datasource(String name, String metricName, long heartbeat) {
        return datasource(name, metricName, Duration.millis(heartbeat));
    }

    public QueryDescriptor datasource(String name, String metricName, Duration heartbeat) {
        return datasource(new Datasource(name, metricName, heartbeat));
    }

    public QueryDescriptor datasource(Datasource ds) {
        throw new UnsupportedOperationException();
    }

    public QueryDescriptor average(String name, String source) {
        return aggregate(new Aggregate(Function.AVERGE, name, source));
    }

    public QueryDescriptor min(String name, String source) {
        return aggregate(new Aggregate(Function.MINIMUM, name, source));
    }

    public QueryDescriptor max(String name, String source) {
        return aggregate(new Aggregate(Function.MAXIMUM, name, source));
    }

    public QueryDescriptor aggregate(Aggregate aggregate) {
        throw new UnsupportedOperationException();
    }

}
