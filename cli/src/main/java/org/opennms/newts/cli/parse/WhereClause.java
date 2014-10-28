package org.opennms.newts.cli.parse;


import org.opennms.newts.api.Timestamp;

import com.google.common.base.Optional;


class WhereClause {

    private org.opennms.newts.api.Resource m_resource;
    private Timestamp m_rangeStart, m_rangeEnd;

    void and(Relation r) {
        if (r instanceof Resource) {
            m_resource = ((Resource) r).getResource();
        }
        else if (r instanceof RangeStart) {
            m_rangeStart = ((RangeStart) r).getTimestamp();
        }
        else if (r instanceof RangeEnd) {
            m_rangeEnd = ((RangeEnd) r).getTimestamp();
        }
        else {
            throw new RuntimeException("Unknown relation type; Report as BUG!");
        }
    }

    org.opennms.newts.api.Resource getResource() {
        return m_resource;
    }

    Optional<Timestamp> getRangeEnd() {
        return m_rangeEnd != null ? Optional.of(m_rangeEnd) : Optional.<Timestamp> absent();
    }

    Optional<Timestamp> getRangeStart() {
        return m_rangeStart != null ? Optional.of(m_rangeStart) : Optional.<Timestamp> absent();
    }

}
