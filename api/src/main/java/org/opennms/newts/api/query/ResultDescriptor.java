package org.opennms.newts.api.query;


import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import org.opennms.newts.api.Duration;
import org.opennms.newts.api.query.Datasource.AggregationFunction;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;


public class ResultDescriptor {

    public static interface Calculation {
        double apply(double... ds);
    }

    public static interface UnaryFunction {
        double apply(double a);
    }

    public static interface BinaryFunction {
        double apply(double a, double b);
    }

    public static class CalculationDescriptor {
        private String m_label;
        private Calculation m_calculation;
        private String[] m_args;

        public CalculationDescriptor(String label, Calculation calculation, String... args) {
            m_label = label;
            m_calculation = calculation;
            m_args = args;
        }

        public String getLabel() {
            return m_label;
        }

        public Calculation getCalculation() {
            return m_calculation;
        }

        public String[] getArgs() {
            return m_args;
        }

    }

    /**
     * The default step size in milliseconds.
     */
    public static final int DEFAULT_STEP = 300000;

    /**
     * Multiple of the step size to use as default heartbeat.
     */
    public static final int DEFAULT_HEARTBEAT_MULTIPLIER = 2;

    /**
     * Default X Files Factor (percentage of NaN pdps that are allowed when aggregating)
     */
    public static final double DEFAULT_XFF = 0.5;

    private Duration m_step;
    private final Map<String, Datasource> m_datasources = Maps.newHashMap();
    private final Map<String, CalculationDescriptor> m_calculations = Maps.newHashMap();

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

    public Map<String, CalculationDescriptor> getCalculations() {
        return m_calculations;
    }

    /**
     * Returns the set of unique source names; The names of the underlying samples used as the
     * source of aggregations.
     * 
     * @return source names
     */
    public Set<String> getSourceNames() {
        return Sets.newHashSet(Iterables.transform(getDatasources().values(), new Function<Datasource, String>() {

            @Override
            public String apply(Datasource input) {
                return input.getSource();
            }
        }));
    }

    public Set<String> getLabels() {
        return Sets.union(m_datasources.keySet(), m_calculations.keySet());
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

    public ResultDescriptor datasource(String metricName, AggregationFunction aggregationFunction) {
        return datasource(metricName, metricName, aggregationFunction);
    }

    public ResultDescriptor datasource(String name, String metricName, AggregationFunction aggregationFunction) {
        return datasource(name, metricName, getStep().times(DEFAULT_HEARTBEAT_MULTIPLIER), aggregationFunction);
    }

    public ResultDescriptor datasource(String name, String metricName, long heartbeat, AggregationFunction aggregationFunction) {
        return datasource(name, metricName, Duration.millis(heartbeat), aggregationFunction);
    }

    public ResultDescriptor datasource(String name, String metricName, Duration heartbeat, AggregationFunction aggregationFunction) {
        return datasource(new Datasource(name, metricName, heartbeat, DEFAULT_XFF, aggregationFunction));
    }

    ResultDescriptor datasource(Datasource ds) {
        checkNotNull(ds, "data source argument");
        checkArgument(!getLabels().contains(ds.getLabel()), "label \"%s\" already in use", ds.getLabel());

        getDatasources().put(ds.getLabel(), ds);

        return this;
    }

    public ResultDescriptor export(String... names) {
        checkLabels(names);
        getExports().addAll(Arrays.asList(names));
        return this;
    }

    private void checkLabels(String... names) {
        Set<String> missing = Sets.newHashSet(names);
        missing.removeAll(getLabels());

        if (missing.size() > 0) {
            throw new IllegalArgumentException(String.format("No such labels(s): %s", missing));
        }
    }

    public ResultDescriptor calculate(CalculationDescriptor calculation) {
        m_calculations.put(calculation.getLabel(), calculation);
        return this;
    }

    public ResultDescriptor calculate(String label, Calculation calculation, String... args) {
        return calculate(new CalculationDescriptor(label, calculation, args));
    }

    public ResultDescriptor calculate(String label, final BinaryFunction binaryFunction, String arg1, String arg2) {
        Calculation calculation = new Calculation() {

            @Override
            public double apply(double... ds) {
                checkArgument(ds.length == 2, "binaryFunctions expect to take exactly two arguments but we've been passed "
                        + ds.length);
                return binaryFunction.apply(ds[0], ds[1]);
            }
        };
        return calculate(label, calculation, arg1, arg2);

    }

    public ResultDescriptor calculate(String label, final UnaryFunction unaryFunction, String arg) {
        Calculation calculation = new Calculation() {

            @Override
            public double apply(double... ds) {
                checkArgument(ds.length == 1, "unaryFunctions expect to take exactly one argument but we've been passed "
                        + ds.length);
                return unaryFunction.apply(ds[0]);
            }
        };
        return calculate(label, calculation, arg);

    }

    public ResultDescriptor sum(String label, String arg1, String arg2) {
        BinaryFunction func = new BinaryFunction() {

            @Override
            public double apply(double a, double b) {
                return a + b;
            }
        };
        return calculate(label, func, arg1, arg2);
    }

}
