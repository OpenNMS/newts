package org.opennms.newts.persistence.leveldb;


import static org.opennms.newts.persistence.leveldb.Utils.assertRowsEqual;

import java.util.Iterator;

import org.junit.Test;
import org.opennms.newts.api.Measurement;
import org.opennms.newts.api.Results.Row;
import org.opennms.newts.api.query.ResultDescriptor;
import org.opennms.newts.persistence.leveldb.Export;
import org.opennms.newts.persistence.leveldb.Utils.MeasurementRowsBuilder;

public class ExportTest {

    @Test
    public void test() {

        // Rows with measurements for "m0", "m1", and "m2".
        Iterator<Row<Measurement>> testData = new MeasurementRowsBuilder("localhost")
                .row(  1).element("m0", 1).element("m1", 2).element("m2", 3)
                .row(300).element("m0", 1).element("m1", 2).element("m2", 3)
                .row(600).element("m0", 1).element("m1", 2).element("m2", 3)
                .build();

        // ResultDescriptor that exports only "m1".
        ResultDescriptor rDescriptor = new ResultDescriptor().datasource("m1", null).export("m1");

        // Expected results.
        Iterator<Row<Measurement>> expected = new MeasurementRowsBuilder("localhost")
                .row(  1).element("m1", 2)
                .row(300).element("m1", 2)
                .row(600).element("m1", 2)
                .build();

        assertRowsEqual(expected, new Export(rDescriptor.getExports(), testData));

    }

}
