package org.opennms.newts.api;

import java.util.Collection;

import com.google.common.base.Optional;

public interface MeasurementRepository {
    public Results select(String resource, Optional<Timestamp> start, Optional<Timestamp> end);
    public void insert(Collection<Measurement> measurements);
}
