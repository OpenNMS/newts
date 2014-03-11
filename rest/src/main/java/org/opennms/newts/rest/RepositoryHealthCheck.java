package org.opennms.newts.rest;


import static com.google.common.base.Preconditions.checkNotNull;

import org.opennms.newts.api.SampleRepository;
import org.opennms.newts.api.Timestamp;

import com.google.common.base.Optional;
import com.yammer.metrics.core.HealthCheck;


public class RepositoryHealthCheck extends HealthCheck {

    private final SampleRepository m_repository;

    protected RepositoryHealthCheck(SampleRepository repository) {
        super("repository");
        m_repository = checkNotNull(repository, "repository argument");
    }

    /** Perform simple query; Establishes only that we can go to the database without excepting. */
    @Override
    protected Result check() throws Exception {

        m_repository.select("notreal", Optional.of(Timestamp.fromEpochMillis(0)), Optional.of(Timestamp.fromEpochMillis(0)));

        return Result.healthy();
    }

}
