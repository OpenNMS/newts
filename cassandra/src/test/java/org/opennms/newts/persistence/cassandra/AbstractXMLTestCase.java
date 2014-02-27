package org.opennms.newts.persistence.cassandra;


import static org.opennms.newts.persistence.cassandra.Utils.assertRowsEqual;
import static org.opennms.newts.persistence.cassandra.Utils.getTestSpecification;

import org.opennms.newts.api.Measurement;
import org.opennms.newts.api.Results.Row;


abstract class AbstractXMLTestCase {

    abstract Iterable<Row<Measurement>> getIterator(XMLTestSpecification testSpec);

    void execute(String name) {
        execute(getTestSpecification(name));
    }

    void execute(XMLTestSpecification testSpec) {
        assertRowsEqual(testSpec.getExpected(), getIterator(testSpec));
    }

}
