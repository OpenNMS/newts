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
package org.opennms.newts.aggregate;


import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

import org.opennms.newts.api.Duration;
import org.opennms.newts.api.Measurement;
import org.opennms.newts.api.Resource;
import org.opennms.newts.api.Sample;
import org.opennms.newts.api.MetricType;
import org.opennms.newts.api.Results.Row;
import org.opennms.newts.api.Timestamp;
import org.opennms.newts.api.ValueType;
import org.opennms.newts.api.query.Datasource;
import org.opennms.newts.api.query.ResultDescriptor;

import com.google.common.collect.Maps;


/**
 * Generate primary data point measurements from a stream of samples.
 *
 * @author eevans
 */
class PrimaryData implements Iterator<Row<Measurement>>, Iterable<Row<Measurement>> {

    private class Accumulation {
        private long m_known, m_unknown;
        private ValueType<?> m_value;
        private Map<String, String> m_attributes = Maps.newHashMap();

        private Accumulation() {
            reset();
        }

        private Accumulation accumulateValue(Duration elapsed, Duration heartbeat, ValueType<?> value) {
            if (elapsed.lt(heartbeat)) {
                m_known += elapsed.asMillis();
                m_value = m_value.plus(value.times(elapsed.asMillis()));
            }
            else {
                m_unknown += elapsed.asMillis();
            }
            return this;
        }

        private Accumulation accumlateAttrs(Map<String, String> attributes) {
            if (attributes != null) m_attributes.putAll(attributes);
            return this;
        }

        private Double getAverage() {
            return isValid() ? m_value.divideBy(m_known).doubleValue() : Double.NaN;
        }

        private long getKnown() {
            return m_known;
        }

        private long getUnknown() {
            return m_unknown;
        }

        private double getElapsed() {
            return getKnown() + getUnknown();
        }

        private boolean isValid() {
            return getUnknown() < (getElapsed() / 2);
        }

        private void reset() {
            m_known = m_unknown = 0;
            m_value = ValueType.compose(0, MetricType.GAUGE);
            m_attributes = Maps.newHashMap();
        }

        private Map<String, String> getAttributes() {
            return m_attributes;
        }

    }

    private final ResultDescriptor m_resultDescriptor;
    private final Resource m_resource;
    private final Iterator<Timestamp> m_timestamps;
    private final Duration m_interval;
    private final Iterator<Row<Sample>> m_input;
    private final Map<String, Sample> m_lastUpdates = Maps.newHashMap();
    private final Map<String, Accumulation> m_accumulation = Maps.newHashMap();

    private Row<Sample> m_current = null;

    PrimaryData(Resource resource, Timestamp start, Timestamp end, ResultDescriptor resultDescriptor, Iterator<Row<Sample>> input) {
        m_resultDescriptor = checkNotNull(resultDescriptor, "result descriptor argument");
        m_resource = checkNotNull(resource, "resource argument");
        checkNotNull(start, "start argument");
        checkNotNull(end, "end argument");
        m_input = checkNotNull(input, "input argument");

        m_interval = resultDescriptor.getInterval();

        m_timestamps = new IntervalGenerator(start.stepFloor(m_interval), end.stepCeiling(m_interval), m_interval);

        if (m_input.hasNext()) m_current = m_input.next();

    }

    @Override
    public boolean hasNext() {
        return m_timestamps.hasNext();
    }

    @Override
    public Row<Measurement> next() {

        if (!hasNext()) throw new NoSuchElementException();

        Row<Measurement> output = new Row<>(m_timestamps.next(), m_resource);

        while (m_current != null) {
            accumulate(m_current, output.getTimestamp());

            if (m_current.getTimestamp().gte(output.getTimestamp())) {
                break;
            }

            if (m_input.hasNext()) {
                m_current = m_input.next();
            }
            else m_current = null;

        }

        // Go time; We've accumulated enough to produce the output row
        for (Datasource ds : m_resultDescriptor.getDatasources().values()) {

            Accumulation accumulation = getOrCreateAccumulation(ds.getSource());

            // Add sample with accumulated value to output row
            output.addElement(new Measurement(
                    output.getTimestamp(),
                    output.getResource(),
                    ds.getSource(),
                    accumulation.getAverage(),
                    accumulation.getAttributes()));

            // If input is greater than row, accumulate remainder for next row
            if (m_current != null) {

                accumulation.reset();

                Sample sample = m_current.getElement(ds.getSource());

                if (sample == null) {
                    continue;
                }

                if (m_current.getTimestamp().gt(output.getTimestamp())) {
                    Duration elapsed = m_current.getTimestamp().minus(output.getTimestamp());
                    accumulation.accumulateValue(elapsed, ds.getHeartbeat(), sample.getValue());
                    accumulation.accumlateAttrs(sample.getAttributes());
                }

            } else {
                accumulation.reset();
            }
        }

        return output;
    }

    private void accumulate(Row<Sample> row, Timestamp intervalCeiling) {

        for (Datasource ds : m_resultDescriptor.getDatasources().values()) {
            Sample current, last;

            current = row.getElement(ds.getSource());

            if (current == null) {
                continue;
            }

            last = m_lastUpdates.get(current.getName());

            if (last == null) {
                m_lastUpdates.put(current.getName(), current);
                continue;
            }

            // Accumulate nothing when samples are beyond this interval
            if (intervalCeiling.lt(last.getTimestamp())) {
                continue;
            }

            Duration elapsed;

            if (current.getTimestamp().gt(intervalCeiling)) {
                elapsed = intervalCeiling.minus(last.getTimestamp());
            }
            else {
                elapsed = current.getTimestamp().minus(last.getTimestamp());
            }

            getOrCreateAccumulation(current.getName())
                    .accumulateValue(elapsed, ds.getHeartbeat(), current.getValue())
                    .accumlateAttrs(current.getAttributes());

            // Postpone storing as lastUpdate, we'll need this sample again...
            if (!current.getTimestamp().gt(intervalCeiling.plus(m_interval))) {
                m_lastUpdates.put(current.getName(), current);
            }
        }
    }

    private Accumulation getOrCreateAccumulation(String name) {
        Accumulation result = m_accumulation.get(name);

        if (result == null) {
            result = new Accumulation();
            m_accumulation.put(name, result);
        }

        return result;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<Row<Measurement>> iterator() {
        return this;
    }

}
