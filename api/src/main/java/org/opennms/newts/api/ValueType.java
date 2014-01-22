package org.opennms.newts.api;

import java.nio.ByteBuffer;

import com.google.common.primitives.UnsignedLong;


public abstract class ValueType<T extends Number> extends Number {

    private static final long serialVersionUID = 1L;

    public abstract ValueType<T> plus(Number value);

    public abstract ValueType<T> minus(Number value);

    public abstract ValueType<T> delta(Number value);

    public abstract ValueType<T> times(Number value);

    public abstract ValueType<T> divideBy(Number value);

    abstract T getValue();

    public abstract MetricType getType();

    @Override
    public abstract int intValue();

    @Override
    public abstract long longValue();

    @Override
    public abstract float floatValue();

    @Override
    public abstract double doubleValue();

    @Override
    public boolean equals(Object o) {
        if (o == null) return false;
        if (!(o instanceof ValueType)) return false;
        return getValue().equals(((ValueType<?>)o).getValue());
    }

    @Override
    public String toString() {
        return getValue().toString();
    }

    public static ValueType<?> compose(Number number, MetricType type) {
        switch (type) {
            case ABSOLUTE:
                return new Absolute(UnsignedLong.fromLongBits(number.longValue()));
            case COUNTER:
                return new Counter(UnsignedLong.fromLongBits(number.longValue()));
            case DERIVE:
                return new Derive(UnsignedLong.fromLongBits(number.longValue()));
            case GAUGE:
                return new Gauge(number.doubleValue());
            default:
                throw new IllegalArgumentException(String.format("Unknown metric type: %s", type));
        }
    }

    public static ValueType<?> compose(ByteBuffer data, MetricType type) {

        ByteBuffer buf = data.duplicate();

        switch (type) {
            case ABSOLUTE:
                return new Absolute(UnsignedLong.fromLongBits(buf.getLong()));
            case COUNTER:
                return new Counter(UnsignedLong.fromLongBits(buf.getLong()));
            case DERIVE:
                return new Derive(UnsignedLong.fromLongBits(buf.getLong()));
            case GAUGE:
                return new Gauge(buf.getDouble());
            default:
                throw new IllegalArgumentException(String.format("Unknown metric type: %s", type));
        }
    }

    public static ByteBuffer decompose(ValueType<?> value) {
        switch (value.getType()) {
            case ABSOLUTE:
            case COUNTER:
            case DERIVE:
                return (ByteBuffer) ByteBuffer.allocate(8).putLong(0, value.longValue()).rewind();
            case GAUGE:
                return (ByteBuffer) ByteBuffer.allocate(8).putDouble(0, value.doubleValue()).rewind();
            default:
                throw new IllegalArgumentException(String.format("Unknown metric type: %s", value.getType()));
        }
    }

}
