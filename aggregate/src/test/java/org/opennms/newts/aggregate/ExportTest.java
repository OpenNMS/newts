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


import static org.opennms.newts.aggregate.Utils.assertRowsEqual;

import java.util.Iterator;

import org.junit.Test;
import org.opennms.newts.aggregate.Utils.MeasurementRowsBuilder;
import org.opennms.newts.api.Measurement;
import org.opennms.newts.api.Resource;
import org.opennms.newts.api.Results.Row;
import org.opennms.newts.api.query.ResultDescriptor;

public class ExportTest {

    @Test
    public void test() {

        // Rows with measurements for "m0", "m1", and "m2".
        Iterator<Row<Measurement>> testData = new MeasurementRowsBuilder(new Resource("localhost"))
                .row(  1).element("m0", 1).element("m1", 2).element("m2", 3)
                .row(300).element("m0", 1).element("m1", 2).element("m2", 3)
                .row(600).element("m0", 1).element("m1", 2).element("m2", 3)
                .build();

        // ResultDescriptor that exports only "m1".
        ResultDescriptor rDescriptor = new ResultDescriptor().datasource("m1", null).export("m1");

        // Expected results.
        Iterator<Row<Measurement>> expected = new MeasurementRowsBuilder(new Resource("localhost"))
                .row(  1).element("m1", 2)
                .row(300).element("m1", 2)
                .row(600).element("m1", 2)
                .build();

        assertRowsEqual(expected, new Export(rDescriptor.getExports(), testData));

    }

}
