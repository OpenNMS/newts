package org.opennms.newts.api.query;

public interface CalculationFunction {
    double apply(double... ds);
}
