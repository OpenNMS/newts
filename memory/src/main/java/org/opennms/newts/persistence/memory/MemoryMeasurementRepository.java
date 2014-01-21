package org.opennms.newts.persistence.memory;


import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.opennms.newts.api.Measurement;
import org.opennms.newts.api.MeasurementRepository;
import org.opennms.newts.api.Results;
import org.opennms.newts.api.Timestamp;

import com.google.common.base.Optional;
import com.google.common.collect.HashMultimap;


public class MemoryMeasurementRepository implements MeasurementRepository {

    private Map<String, HashMultimap<Timestamp, Measurement>> m_storage = new HashMap<String, HashMultimap<Timestamp, Measurement>>();

    @Override
    public Results select(String resource, Optional<Timestamp> start, Optional<Timestamp> end) {

        Timestamp lower = start.isPresent() ? start.get() : new Timestamp(Long.MIN_VALUE, TimeUnit.MILLISECONDS);
        Timestamp upper = end.isPresent() ? end.get() : new Timestamp(Long.MAX_VALUE, TimeUnit.MILLISECONDS);

        Results r = new Results();

        for (Entry<Timestamp, Measurement> entry : m_storage.get(resource).entries()) {
            if (entry.getKey().gte(lower) && entry.getKey().lte(upper)) {
                r.addMeasurement(entry.getValue());
            }
        }

        return r;
    }

    @Override
    public void insert(Collection<Measurement> measurements) {

        for (Measurement m : measurements) {
            if (!(m_storage.containsKey(m.getResource()))) {
                m_storage.put(m.getResource(), HashMultimap.<Timestamp, Measurement> create());
            }

            m_storage.get(m.getResource()).put(m.getTimestamp(), m);
        }

    }

}
