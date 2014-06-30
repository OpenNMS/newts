package org.opennms.newts.stress;


import static com.google.common.base.Preconditions.checkNotNull;

import org.opennms.newts.api.Duration;
import org.opennms.newts.api.Timestamp;

import com.google.common.base.Optional;


class Query {

    private final String m_resource;
    private final Timestamp m_start;
    private final Timestamp m_end;
    private final Duration m_resolution;

    Query(String resource, Timestamp start, Timestamp end, Duration resolution) {
        m_resource = checkNotNull(resource, "resource argument");
        m_start = checkNotNull(start, "start argument");
        m_end = checkNotNull(end, "end argument");
        m_resolution = checkNotNull(resolution, "resolution argument");
    }

    String getResource() {
        return m_resource;
    }

    Optional<Timestamp> getStart() {
        return Optional.of(m_start);
    }

    Optional<Timestamp> getEnd() {
        return Optional.of(m_end);
    }

    Duration getResolution() {
        return m_resolution;
    }

}
