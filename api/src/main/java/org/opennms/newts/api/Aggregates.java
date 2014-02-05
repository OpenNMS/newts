package org.opennms.newts.api;


import java.util.Arrays;
import java.util.EnumSet;


public class Aggregates {

    public enum Function {
        AVERAGE, RATE;
    }

    public static final int DEFAULT_STEP_SECONDS = 300;

    private Duration m_step;
    private EnumSet<Function> m_functions;

    public Aggregates() {
        this(DEFAULT_STEP_SECONDS, Function.AVERAGE);
    }

    public Aggregates(int stepSeconds, Function... functions) {
        m_step = Duration.seconds(stepSeconds);
        m_functions = EnumSet.copyOf(Arrays.asList(functions));
    }

    public Aggregates step(int seconds) {
        m_step = Duration.seconds(seconds);
        return this;
    }

    public Aggregates average() {
        m_functions.add(Function.AVERAGE);
        return this;
    }

    public Aggregates rate() {
        m_functions.add(Function.RATE);
        return this;
    }

    public Duration getStep() {
        return m_step;
    }

    public EnumSet<Function> getFunctions() {
        return m_functions;
    }

}
