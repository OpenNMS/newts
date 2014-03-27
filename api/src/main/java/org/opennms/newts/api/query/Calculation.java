package org.opennms.newts.api.query;


import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Arrays;


public class Calculation {

    private final String m_label;
    private final CalculationFunction m_calculationFunction;
    private final String[] m_args;

    public Calculation(String label, CalculationFunction calculationFunction, String... args) {
        m_label = checkNotNull(label, "label argument");
        m_calculationFunction = checkNotNull(calculationFunction, "calculation function argument");

        checkArgument(args.length > 0, "one or more function arguments are required");
        m_args = args;

    }

    public String getLabel() {
        return m_label;
    }

    public CalculationFunction getCalculationFunction() {
        return m_calculationFunction;
    }

    public String[] getArgs() {
        return m_args;
    }

    @Override
    public String toString() {
        return String.format(
                "%s[%s, function=%s, args=%s]",
                getClass().getSimpleName(),
                getLabel(),
                getCalculationFunction(),
                Arrays.asList(getArgs()));
    }

}
