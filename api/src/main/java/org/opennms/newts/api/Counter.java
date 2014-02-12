package org.opennms.newts.api;


import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.primitives.UnsignedLong;


public class Counter extends ValueType<UnsignedLong> {

    private static final long serialVersionUID = 1L;
    private static final UnsignedLong MAX32 = toUnsignedLong(0xFFFFFFFFL);
    private static final UnsignedLong MAX64 = toUnsignedLong(0xFFFFFFFFFFFFFFFFL);

    private final UnsignedLong m_value;

    public Counter(long value) {
        this(UnsignedLong.fromLongBits(value));
    }

    public Counter(UnsignedLong value) {
        m_value = checkNotNull(value, "value");
    }

    @Override
    public Counter plus(Number value) {
        return new Counter(getValue().plus(toUnsignedLong(value)));
    }

    @Override
    public Counter minus(Number value) {
        return new Counter(getValue().minus(toUnsignedLong(value)));
    }

    @Override
    public ValueType<UnsignedLong> delta(Number value) {
        UnsignedLong difference = getValue().minus(toUnsignedLong(value));

        if (isNegative(difference)) {
            UnsignedLong difference32 = difference.plus(MAX32).plus(UnsignedLong.ONE);

            if (isNegative(difference32)) {
                return new Counter(difference.plus(MAX64).plus(UnsignedLong.ONE));
            }
            else {
                return new Counter(difference32);
            }
        }
        else {
            return new Counter(difference);
        }

    }

    private boolean isNegative(UnsignedLong val) {
        return val.compareTo(UnsignedLong.ZERO) < 0;
    }

    @Override
    public Counter times(Number value) {
        return new Counter(getValue().times(toUnsignedLong(value)));
    }

    @Override
    public Counter divideBy(Number value) {
        return new Counter(getValue().dividedBy(toUnsignedLong(value)));
    }

    @Override
    UnsignedLong getValue() {
        return m_value;
    }

    @Override
    public MetricType getType() {
        return MetricType.COUNTER;
    }

    @Override
    public int intValue() {
        return getValue().intValue();
    }

    @Override
    public long longValue() {
        return getValue().longValue();
    }

    @Override
    public float floatValue() {
        return getValue().floatValue();
    }

    @Override
    public double doubleValue() {
        return getValue().doubleValue();
    }

    private static UnsignedLong toUnsignedLong(Number value) {
        if (value instanceof UnsignedLong) return (UnsignedLong) value;
        return UnsignedLong.fromLongBits(value.longValue());
    }

}
