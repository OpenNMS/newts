package org.opennms.newts.api;

import java.util.Collection;

public interface MeasurementRepository {
    public Results select(String resource, Timestamp start, Timestamp end);
    public void insert(Collection<Measurement> measurements);
}
