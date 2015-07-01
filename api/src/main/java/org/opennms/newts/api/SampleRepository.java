/*
 * Copyright 2015, The OpenNMS Group
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
package org.opennms.newts.api;


import java.util.Collection;

import org.opennms.newts.api.query.ResultDescriptor;

import com.google.common.base.Optional;


public interface SampleRepository {

    /**
     * Query measurements.
     * 
     * @param resource
     *            name of the sampled resource
     * @param start
     *            query start time (defaults to 24 hours less than {@code end}, if absent)
     * @param end
     *            query end time (defaults to current time if absent)
     * @param descriptor
     *            aggregation descriptor
     * @param resolution
     *            temporal resolution of results (defaults to a value resulting in 1-10 measurements, if absent)
     * @return query results
     */
    public Results<Measurement> select(Resource resource, Optional<Timestamp> start, Optional<Timestamp> end, ResultDescriptor descriptor, Optional<Duration> resolution);

    /**
     * Read stored samples.
     * 
     * @param resource
     *            name of the sampled resource
     * @param start
     *            query start time (defaults to 24 hours less than {@code end}, if absent)
     * @param end
     *            query end time (defaults to current time if absent)
     * @return query results
     */
    public Results<Sample> select(Resource resource, Optional<Timestamp> start, Optional<Timestamp> end);

    /**
     * Write (store) samples.
     * 
     * @param samples
     */
    public void insert(Collection<Sample> samples);

    /**
     * Write (store) samples.
     *
     * @param samples
     * @param calculateTimeToLive
     */
    public void insert(Collection<Sample> samples, boolean calculateTimeToLive);

}
