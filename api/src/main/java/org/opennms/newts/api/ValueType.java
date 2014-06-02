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
    
    public boolean isNan() {
        return Double.isNaN(doubleValue());
    }

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

    public static ValueType<?> compose(ByteBuffer data) {

        ByteBuffer buffer = data.duplicate();
        MetricType type = MetricType.fromCode(buffer.get());

        switch (type) {
            case ABSOLUTE:
                return new Absolute(UnsignedLong.fromLongBits(buffer.getLong()));
            case COUNTER:
                return new Counter(UnsignedLong.fromLongBits(buffer.getLong()));
            case DERIVE:
                return new Derive(UnsignedLong.fromLongBits(buffer.getLong()));
            case GAUGE:
                return new Gauge(buffer.getDouble());
            default:
                throw new IllegalArgumentException(String.format("Unknown metric type: %s", type));
        }
    }

    public static ByteBuffer decompose(ValueType<?> value) {
        ByteBuffer buffer;

        switch (value.getType()) {
            case ABSOLUTE:
            case COUNTER:
            case DERIVE:
                buffer = ByteBuffer.allocate(9);
                buffer.put(0, value.getType().getCode());
                buffer.putLong(1, value.longValue());
                buffer.rewind();
                return buffer;
            case GAUGE:
                buffer = ByteBuffer.allocate(9);
                buffer.put(0, value.getType().getCode());
                buffer.putDouble(1, value.doubleValue());
                buffer.rewind();
                return buffer;
            default:
                throw new IllegalArgumentException(String.format("Unknown metric type: %s", value.getType()));
        }
    }

}
