/*
 * Copyright 2014, The OpenNMS Group
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.opennms.newts.api.query;


import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import org.apache.commons.jexl2.Expression;
import org.apache.commons.jexl2.JexlContext;
import org.apache.commons.jexl2.JexlEngine;
import org.apache.commons.jexl2.MapContext;
import org.opennms.newts.api.Duration;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;


public class ResultDescriptor implements Serializable {
    private static final long serialVersionUID = -6983442401680715547L;

    public static interface UnaryFunction extends Serializable {
        double apply(double a);
    }

    public static interface BinaryFunction extends Serializable {
        double apply(double a, double b);
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

    private Duration m_interval;
    private final Map<String, Datasource> m_datasources = Maps.newHashMap();
    // use linkedHashMap so creation order is preserved. calculations can only depend on earlier not later calculations
    private final Map<String, Calculation> m_calculations = Maps.newLinkedHashMap();

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
        m_interval = step;
    }

    public Duration getInterval() {
        return m_interval;
    }

    public Map<String, Datasource> getDatasources() {
        return m_datasources;
    }

    public Map<String, Calculation> getCalculations() {
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
        m_interval = step;
        return this;
    }

    public ResultDescriptor datasource(String metricName, AggregationFunction aggregationFunction) {
        return datasource(metricName, metricName, aggregationFunction);
    }

    public ResultDescriptor datasource(String name, String metricName, AggregationFunction aggregationFunction) {
        return datasource(name, metricName, getInterval().times(DEFAULT_HEARTBEAT_MULTIPLIER), aggregationFunction);
    }

    public ResultDescriptor datasource(String name, String metricName, long heartbeat, AggregationFunction aggregationFunction) {
        return datasource(name, metricName, Duration.millis(heartbeat), aggregationFunction);
    }

    public ResultDescriptor datasource(String name, String metricName, Duration heartbeat, AggregationFunction aggregationFunction) {
        return datasource(name, metricName, heartbeat, DEFAULT_XFF, aggregationFunction);
    }

    public ResultDescriptor datasource(String name, String metricName, Duration heartbeat, double xff, AggregationFunction aggregationFunction) {
        return datasource(new Datasource(name, metricName, heartbeat, xff, aggregationFunction));
    }

    ResultDescriptor datasource(Datasource ds) {
        checkNotNull(ds, "data source argument");
        checkArgument(!getLabels().contains(ds.getLabel()), "label \"%s\" already in use", ds.getLabel());
        checkArgument(ds.getHeartbeat().gte(getInterval()), "heartbeat cannot be smaller than sample interval");

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
    
    private boolean isNumber(String number) {
        try {
            Double.parseDouble(number);
            return true;
        } catch(NumberFormatException e) {
            return false;
        }
    }
     
    private void checkValues(String... values) {
        for(String value : values) {
            if (!isNumber(value)) {
                checkLabels(value);
            }
        }
    }

    public ResultDescriptor calculate(Calculation calculation) {
        checkValues(calculation.getArgs());
        checkArgument(!getLabels().contains(calculation.getLabel()), "label \"%s\" already in use", calculation.getLabel());
        m_calculations.put(calculation.getLabel(), calculation);
        return this;
    }

    public ResultDescriptor calculate(String label, CalculationFunction calculationFunction, String... args) {
        return calculate(new Calculation(label, calculationFunction, args));
    }

    public ResultDescriptor calculate(String label, final BinaryFunction binaryFunction, String arg1, String arg2) {
        CalculationFunction calculationFunction = new CalculationFunction() {
            private static final long serialVersionUID = -4149546450724920349L;

            @Override
            public double apply(double... ds) {
                checkArgument(ds.length == 2, "binaryFunctions expect to take exactly two arguments but we've been passed "
                        + ds.length);
                return binaryFunction.apply(ds[0], ds[1]);
            }
        };
        return calculate(label, calculationFunction, arg1, arg2);

    }

    public ResultDescriptor calculate(String label, final UnaryFunction unaryFunction, String arg) {
        CalculationFunction calculationFunction = new CalculationFunction() {
            private static final long serialVersionUID = 2353112913166459161L;

            @Override
            public double apply(double... ds) {
                checkArgument(ds.length == 1, "unaryFunctions expect to take exactly one argument but we've been passed "
                        + ds.length);
                return unaryFunction.apply(ds[0]);
            }
        };
        return calculate(label, calculationFunction, arg);

    }
    
    public ResultDescriptor expression(String label, String expression) {
        final JexlEngine je = new JexlEngine();
        final Expression expr = je.createExpression(expression);
        final String[] labels = getLabels().toArray(new String[0]);
        CalculationFunction evaluate = new CalculationFunction() {
            private static final long serialVersionUID = -3328049421398096252L;

            @Override
            public double apply(double... ds) {
                JexlContext jc = new MapContext();
                for(int i = 0; i < labels.length; i++) {
                    jc.set(labels[i], ds[i]);
                }
                return ((Number)expr.evaluate(jc)).doubleValue();
                
            }
        };
        return calculate(label, evaluate, labels);
    }

    @Override
    public String toString() {
        return String.format(
                "%s[interval=%s, datasources=%s, calculations=%s, exports=%s]",
                getClass().getSimpleName(),
                getInterval(),
                getDatasources().values(),
                getCalculations().values(),
                getExports());
    }

}
