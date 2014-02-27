package org.opennms.newts.persistence.cassandra;


import static org.opennms.newts.persistence.cassandra.Utils.getResultDescriptor;

import org.junit.Test;
import org.opennms.newts.api.Measurement;
import org.opennms.newts.api.Results.Row;


public class ExportTest extends AbstractXMLTestCase {

    @Test
    public void testSubset() {
        execute("export/subset.xml");
    }

    @Test
    public void testExportAll() {
        execute("export/all.xml");
    }

    @Override
    Iterable<Row<Measurement>> getIterator(XMLTestSpecification testSpec) {
        return new Export(getResultDescriptor(testSpec).getExports(), testSpec.getTestDataAsMeasurements());
    }

}
