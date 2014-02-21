package org.opennms.newts.persistence.memory;


import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.opennms.newts.api.Duration;
import org.opennms.newts.api.Measurement;
import org.opennms.newts.api.Results;
import org.opennms.newts.api.Sample;
import org.opennms.newts.api.SampleRepository;
import org.opennms.newts.api.Timestamp;
import org.opennms.newts.api.query.ResultDescriptor;

import com.google.common.base.Optional;
import com.google.common.collect.HashMultimap;


public class MemorySampleRepository implements SampleRepository {

    private Map<String, HashMultimap<Timestamp, Sample>> m_storage = new HashMap<String, HashMultimap<Timestamp, Sample>>();

    @Override
    public Results<Measurement> select(String resource, Optional<Timestamp> start, Optional<Timestamp> end, ResultDescriptor descriptor, Duration resolution) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Results<Sample> select(String resource, Optional<Timestamp> start, Optional<Timestamp> end) {

        Timestamp upper = end.isPresent() ? end.get() : Timestamp.now();
        Timestamp lower = start.isPresent() ? start.get() : upper.minus(Duration.seconds(86400));

        Results<Sample> r = new Results<Sample>();

        for (Entry<Timestamp, Sample> entry : m_storage.get(resource).entries()) {
            if (entry.getKey().gte(lower) && entry.getKey().lte(upper)) {
                r.addElement(entry.getValue());
            }
        }

        return r;
    }

    @Override
    public void insert(Collection<Sample> samples) {

        for (Sample m : samples) {
            if (!(m_storage.containsKey(m.getResource()))) {
                m_storage.put(m.getResource(), HashMultimap.<Timestamp, Sample> create());
            }

            m_storage.get(m.getResource()).put(m.getTimestamp(), m);
        }

    }

}
