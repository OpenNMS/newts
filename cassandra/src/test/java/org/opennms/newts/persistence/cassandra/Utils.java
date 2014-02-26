package org.opennms.newts.persistence.cassandra;


import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.propagate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.opennms.newts.api.Duration;
import org.opennms.newts.api.Measurement;
import org.opennms.newts.api.Sample;
import org.opennms.newts.api.Results.Row;


class Utils {

    private static Unmarshaller s_unmarshaller;

    static {
        try {
            s_unmarshaller = JAXBContext.newInstance(XMLTestCase.class).createUnmarshaller();
        }
        catch (JAXBException e) {
            throw propagate(e);
        }
    }

    /**
     * Obtain an {@link XMLTestCase} instance for the specified XML test descriptor.
     *
     * @param name
     *            name of the xml test case
     * @return test case descriptor
     */
    static XMLTestCase getTestCase(String name) {
        String path = String.format("/xml_tests/%s", name);
        InputStream stream = checkNotNull(Utils.class.getResourceAsStream(path), "No such file in classpath: %s", path);

        try {
            return (XMLTestCase) s_unmarshaller.unmarshal(stream);
        }
        catch (JAXBException e) {
            throw propagate(e);
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
    static void assertRowsEqual(Iterable<Row<Measurement>> expectedRows, Iterable<Row<Measurement>> actualRows) {

        Iterator<Row<Measurement>> expectedRowsIter = expectedRows.iterator();

        for (Row<Measurement> actual : actualRows) {
            assertTrue("Extraneous result row(s)", expectedRowsIter.hasNext());

            Row<Measurement> expected = expectedRowsIter.next();

            assertEquals("Unexpected row resource", expected.getResource(), actual.getResource());
            assertEquals("Unexpected row timestamp", expected.getTimestamp(), actual.getTimestamp());
            assertEquals("Measurement count mismatch", expected.getElements().size(), actual.getElements().size());

            for (Measurement m : actual.getElements()) {
                assertSamplesEqual(expected.getElement(m.getName()), m);
            }

        }

        assertFalse("Missing result rows(s)", expectedRowsIter.hasNext());

    }

    /**
     * Assert that two {@link Sample}s are equal.
     *
     * @param expected
     *            expected value
     * @param actual
     *            actual value
     */
    static void assertSamplesEqual(Measurement expected, Measurement actual) {
        assertEquals("Unexpected measurement name", expected.getName(), actual.getName());
        assertEquals("Unexpected measurement resource", expected.getResource(), actual.getResource());
        assertEquals("Unexpected measurement timestamp", expected.getTimestamp(), actual.getTimestamp());
        assertEquals("Incorrect value", expected.getValue().doubleValue(), actual.getValue().doubleValue(), 0.01d);
    }

    @SuppressWarnings("serial")
    static Map<String, Duration> getHeartbeats(final XMLTestCase testCase) {
        return new HashMap<String, Duration>() {
            {
                for (String metric : testCase.getMetrics())
                    put(metric, testCase.getHeartbeat());
            }
        };
    }

}
