package org.opennms.newts.api.query;


import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import org.opennms.newts.api.Duration;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;


public class ResultDescriptor {

    /**
     * The default step size in milliseconds.
     */
    public static final int DEFAULT_STEP = 300000;

    /**
     * Multiple of the step size to use as default heartbeat.
     */
    public static final int DEFAULT_HEARTBEAT_MULTIPLIER = 2;

    private Duration m_step;
    private final Map<String, Datasource> m_datasources = Maps.newHashMap();
    private final Map<String, Aggregate> m_aggregates = Maps.newHashMap();
    private final Set<String> m_sources = Sets.newHashSet();
    private final Set<String> m_exports = Sets.newHashSet();

    /**
     * Constructs a new {@link ResultDescriptor} with the default step size.
     */
    public ResultDescriptor() {
        this(DEFAULT_STEP);
    }

    /**
     * Constructs a new {@link ResultDescriptor} with the given step size.
     * 
     * @param step
     *            duration in milliseconds
     */
    public ResultDescriptor(long step) {
        this(Duration.millis(step));
    }

    /**
     * Constructs a new {@link ResultDescriptor} with the given step size.
     * 
     * @param step
     *            duration as an instance of {@link Duration}
     */
    public ResultDescriptor(Duration step) {
        m_step = step;
    }

    public Duration getStep() {
        return m_step;
    }

    public Map<String, Datasource> getDatasources() {
        return m_datasources;
    }

    public Map<String, Aggregate> getAggregates() {
        return m_aggregates;
    }

    public Set<String> getSources() {
        return m_sources;
    }

    public Set<String> getExports() {
        return m_exports;
    }

    /**
     * Set the step duration.
     * 
     * @param step
     *            duration in milliseconds
     * @return
     */
    public ResultDescriptor step(long step) {
        return step(Duration.millis(step));
    }

    public ResultDescriptor step(Duration step) {
        m_step = step;
        return this;
    }

    public ResultDescriptor datasource(String metricName) {
        return datasource(metricName, metricName);
    }

    public ResultDescriptor datasource(String name, String metricName) {
        return datasource(name, metricName, getStep().times(DEFAULT_HEARTBEAT_MULTIPLIER));
    }

    public ResultDescriptor datasource(String name, String metricName, long heartbeat) {
        return datasource(name, metricName, Duration.millis(heartbeat));
    }

    public ResultDescriptor datasource(String name, String metricName, Duration heartbeat) {
        return datasource(new Datasource(name, metricName, heartbeat));
    }

    public ResultDescriptor datasource(Datasource ds) {
        checkNotNull(ds, "data source argument");
        checkArgument(!getSources().contains(ds.getName()), "source \"%s\" already exists", ds.getName());

        getDatasources().put(ds.getName(), ds);
        getSources().add(ds.getName());

        return this;
    }

    public ResultDescriptor average(String name, String source) {
        return aggregate(new Aggregate(Function.AVERAGE, name, source));
    }

    public ResultDescriptor min(String name, String source) {
        return aggregate(new Aggregate(Function.MINIMUM, name, source));
    }

    public ResultDescriptor max(String name, String source) {
        return aggregate(new Aggregate(Function.MAXIMUM, name, source));
    }

    public ResultDescriptor aggregate(Aggregate aggregate) {
        checkNotNull(aggregate, "aggregate argument");
        checkArgument(!getSources().contains(aggregate.getName()), "source \"%s\" already exists", aggregate.getName());
        checkSources(aggregate.getSources());

        getAggregates().put(aggregate.getName(), aggregate);
        getSources().add(aggregate.getName());

        return this;
    }

    public ResultDescriptor export(String... names) {
        checkSources(names);
        getExports().addAll(Arrays.asList(names));
        return this;
    }

    /** Throw exception if any argument is not a source. */
    private void checkSources(String[] names) {
        Set<String> missing = Sets.newHashSet(names);
        missing.removeAll(getSources());

        if (missing.size() > 0) {
            throw new IllegalArgumentException(String.format("No such source(s): %s", missing));
        }
    }

}
