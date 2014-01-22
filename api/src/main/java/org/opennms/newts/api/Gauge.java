package org.opennms.newts.api;


import static com.google.common.base.Preconditions.checkNotNull;


public class Gauge extends ValueType<Double> {

    private static final long serialVersionUID = 1L;
    private final Double m_value;

    public Gauge(double value) {
        m_value = checkNotNull(value, "value");
    }

    @Override
    public Gauge plus(Number value) {
        return new Gauge(doubleValue() + value.doubleValue());
    }

    @Override
    public Gauge minus(Number value) {
        return new Gauge(doubleValue() - value.doubleValue());
    }

    @Override
    public ValueType<Double> delta(Number value) {
        return minus(value);
    }

    @Override
    public Gauge times(Number value) {
        return new Gauge(doubleValue() * value.doubleValue());
    }

    @Override
    public Gauge divideBy(Number value) {
        return new Gauge(doubleValue() / value.doubleValue());
    }

    @Override
    public Double getValue() {
        return m_value;
    }

    @Override
    public MetricType getType() {
        return MetricType.GAUGE;
    }

    @Override
    public int intValue() {
        return m_value.intValue();
    }

    @Override
    public long longValue() {
        return m_value.longValue();
    }

    @Override
    public float floatValue() {
        return m_value.floatValue();
    }

    @Override
    public double doubleValue() {
        return getValue();
    }

}
