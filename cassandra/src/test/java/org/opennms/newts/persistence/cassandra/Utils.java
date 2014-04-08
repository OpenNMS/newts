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
package org.opennms.newts.persistence.cassandra;


import static com.google.common.base.Preconditions.checkNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;
import java.util.List;

import org.opennms.newts.api.Element;
import org.opennms.newts.api.Measurement;
import org.opennms.newts.api.MetricType;
import org.opennms.newts.api.Results.Row;
import org.opennms.newts.api.Sample;
import org.opennms.newts.api.Timestamp;
import org.opennms.newts.api.ValueType;

import com.google.common.collect.Lists;


class Utils {

    static abstract class AbstractRowsBuilder<T extends Element<?>> {

        private final List<Row<T>> m_results = Lists.newArrayList();
        private final String m_resource;

        private Row<T> m_current;

        AbstractRowsBuilder(String resource) {
            m_resource = checkNotNull(resource, "resource argument");
        }

        AbstractRowsBuilder<T> row(Timestamp timestamp) {
            if (m_current != null && (!timestamp.gt(m_current.getTimestamp()))) {
                throw new IllegalArgumentException("rows must be added in sort order");
            }
            m_current = new Row<>(timestamp, m_resource);
            m_results.add(m_current);
            return this;
        }

        AbstractRowsBuilder<T> row(int epochSeconds) {
            return row(Timestamp.fromEpochSeconds(epochSeconds));
        }

        protected String getResource() {
            return m_resource;
        }

        protected Timestamp getCurrentTimestamp() {
            return m_current.getTimestamp();
        }

        protected void addElement(T element) {
            m_current.addElement(element);
        }

        abstract AbstractRowsBuilder<T> element(String name, double value);

        Iterator<Row<T>> build() {
            return m_results.iterator();
        }

    }

    static class MeasurementRowsBuilder extends AbstractRowsBuilder<Measurement> {

        MeasurementRowsBuilder(String resource) {
            super(resource);
        }

        @Override
        MeasurementRowsBuilder element(String name, double value) {
            addElement(new Measurement(getCurrentTimestamp(), getResource(), name, value));
            return this;
        }

    }

    static class SampleRowsBuilder extends AbstractRowsBuilder<Sample> {

        private final MetricType m_type;

        SampleRowsBuilder(String resource, MetricType type) {
            super(resource);

            m_type = checkNotNull(type, "type argument");
        }

        @Override
        SampleRowsBuilder element(String name, double value) {
            addElement(new Sample(getCurrentTimestamp(), getResource(), name, m_type, ValueType.compose(value, m_type)));
            return this;
        }

    }

    /**
     * Assert that two sets of {@link Row} results are equal.
     * 
     * @param expectedRows
     *            expected value
     * @param actualRows
     *            actual value
     */
    static void assertRowsEqual(Iterator<Row<Measurement>> expectedRows, Iterator<Row<Measurement>> actualRows) {

        while (actualRows.hasNext()) {
            Row<Measurement> actual = actualRows.next();

            assertTrue("Extraneous result row(s)", expectedRows.hasNext());

            Row<Measurement> expected = expectedRows.next();

            assertEquals("Unexpected row resource", expected.getResource(), actual.getResource());
            assertEquals("Unexpected row timestamp", expected.getTimestamp(), actual.getTimestamp());
            assertEquals("Measurement count mismatch", expected.getElements().size(), actual.getElements().size());

            for (Measurement m : actual.getElements()) {
                assertNotNull("Extraneous result measurement(s)", expected.getElement(m.getName()));
                assertSamplesEqual(expected.getElement(m.getName()), m);
            }

        }

        assertFalse("Missing result rows(s)", expectedRows.hasNext());

    }

    /**
     * Assert that two {@link Measurements}s are equal.
     * 
     * @param expected
     *            expected value
     * @param actual
     *            actual value
     */
    static void assertSamplesEqual(Measurement expected, Measurement actual) {
        checkNotNull(expected, "expected");
        checkNotNull(actual, "actual");
        assertEquals("Unexpected measurement name", expected.getName(), actual.getName());
        assertEquals("Unexpected measurement resource", expected.getResource(), actual.getResource());
        assertEquals("Unexpected measurement timestamp", expected.getTimestamp(), actual.getTimestamp());
        assertEquals("Incorrect value", expected.getValue().doubleValue(), actual.getValue().doubleValue(), 0.00000001d);
    }

}
