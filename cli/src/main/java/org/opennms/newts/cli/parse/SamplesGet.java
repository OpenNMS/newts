package org.opennms.newts.cli.parse;


import static com.google.common.base.Preconditions.checkNotNull;

import org.opennms.newts.api.Timestamp;

import com.google.common.base.Optional;


public class SamplesGet extends Statement {

    private WhereClause m_where;

    SamplesGet(WhereClause clause) {
        m_where = checkNotNull(clause, "clause argument");
    }

    public org.opennms.newts.api.Resource getResource() {
        return getWhere().getResource();
    }

    public Optional<Timestamp> getRangeStart() {
        return getWhere().getRangeStart();
    }

    public Optional<Timestamp> getRangeEnd() {
        return getWhere().getRangeEnd();
    }

    WhereClause getWhere() {
        return m_where;
    }

    @Override
    public Type getType() {
        return Type.SAMPLES_GET;
    }

}
