package org.opennms.newts.persistence.cassandra;


import static org.opennms.newts.persistence.cassandra.Utils.assertRowsEqual;
import static org.opennms.newts.persistence.cassandra.Utils.getResultDescriptor;
import static org.opennms.newts.persistence.cassandra.Utils.getTestSpecification;

import java.util.List;

import org.junit.Test;
import org.opennms.newts.api.Measurement;
import org.opennms.newts.api.MetricType;
import org.opennms.newts.api.Results;
import org.opennms.newts.api.Results.Row;
import org.opennms.newts.api.Sample;
import org.opennms.newts.api.query.ResultDescriptor;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;


public class InsertSelectMeasurementsITCase extends AbstractCassandraTestCase {

    @Test
    public void test() {

        XMLTestSpecification testSpec = getTestSpecification("integration/average_samples.xml");
        ResultDescriptor resultDescriptor = getResultDescriptor(testSpec);
        List<Sample> samples = Lists.newArrayList();

        for (Row<Sample> row : testSpec.getTestDataAsSamples(MetricType.GAUGE)) {
            samples.addAll(row.getElements());
        }

        getRepository().insert(samples);

        Results<Measurement> results = getRepository().select(
                testSpec.getResource(),
                Optional.of(testSpec.getStart()),
                Optional.of(testSpec.getEnd()),
                resultDescriptor,
                testSpec.getResolution());

        assertRowsEqual(testSpec.getExpected(), results);

    }

}
