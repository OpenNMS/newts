/*
 * Copyright 2014, The OpenNMS Group
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.opennms.newts.rest;


import static com.google.common.base.Preconditions.checkNotNull;

import org.opennms.newts.api.Resource;
import org.opennms.newts.api.SampleRepository;
import org.opennms.newts.api.Timestamp;

import com.google.common.base.Optional;
import com.codahale.metrics.health.HealthCheck;


public class RepositoryHealthCheck extends HealthCheck {

    private final SampleRepository m_repository;

    protected RepositoryHealthCheck(SampleRepository repository) {
        m_repository = checkNotNull(repository, "repository argument");
    }

    /** Perform simple query; Establishes only that we can go to the database without excepting. */
    @Override
    protected Result check() throws Exception {

        m_repository.select(new Resource("notreal"), Optional.of(Timestamp.fromEpochMillis(0)), Optional.of(Timestamp.fromEpochMillis(0)));

        return Result.healthy();
    }

}
