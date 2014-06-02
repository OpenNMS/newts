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
import java.util.NoSuchElementException;

import org.opennms.newts.api.Measurement;
import org.opennms.newts.api.Results.Row;
import org.opennms.newts.api.query.ResultDescriptor;
import org.opennms.newts.api.query.Calculation;


class Compute implements Iterator<Row<Measurement>>, Iterable<Row<Measurement>> {

    private final ResultDescriptor m_resultDescriptor;
    private final Iterator<Row<Measurement>> m_input;

    Compute(ResultDescriptor resultDescriptor, Iterator<Row<Measurement>> input) {
        m_resultDescriptor = checkNotNull(resultDescriptor, "result descriptor argument");
        m_input = checkNotNull(input, "input argument");
    }

    @Override
    public boolean hasNext() {
        return m_input.hasNext();
    }

    @Override
    public Row<Measurement> next() {

        if (!hasNext()) throw new NoSuchElementException();

        Row<Measurement> row = m_input.next();

        for (Calculation calc : m_resultDescriptor.getCalculations().values()) {
            double v = calc.getCalculationFunction().apply(getValues(row, calc.getArgs()));
            row.addElement(new Measurement(row.getTimestamp(), row.getResource(), calc.getLabel(), v));
        }

        return row;
    }

    private double[] getValues(Row<Measurement> row, String[] names) {
        double[] values = new double[names.length];

        for (int i = 0; i < names.length; i++) {
            values[i] = checkNotNull(row.getElement(names[i]), "Missing measurement; Upstream iterator is bugged").getValue();
        }

        return values;
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
