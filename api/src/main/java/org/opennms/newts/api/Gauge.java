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
    Double getValue() {
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
