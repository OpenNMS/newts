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

import java.util.Collection;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.opennms.newts.api.Context;
import org.opennms.newts.api.Duration;
import org.opennms.newts.api.Resource;
import org.opennms.newts.api.SampleRepository;
import org.opennms.newts.api.Timestamp;
import org.opennms.newts.api.query.ResultDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Optional;


@Path("/measurements")
@Produces(MediaType.APPLICATION_JSON)
public class MeasurementsResource {

    private static final Logger LOG = LoggerFactory.getLogger(MeasurementsResource.class);

    private final SampleRepository m_repository;
    private final Map<String, ResultDescriptorDTO> m_reports;

    public MeasurementsResource(SampleRepository repository, Map<String, ResultDescriptorDTO> reports) {
        m_repository = checkNotNull(repository, "repository argument");
        m_reports = checkNotNull(reports, "reports argument");
    }

    @POST
    @Path("/{resource}")
    @Timed
    public Collection<Collection<MeasurementDTO>> getMeasurements(
            ResultDescriptorDTO descriptorDTO,
            @PathParam("resource") Resource resource,
            @QueryParam("start") Optional<TimestampParam> start,
            @QueryParam("end") Optional<TimestampParam> end,
            @QueryParam("resolution") Optional<DurationParam> resolution,
            @QueryParam("context") Optional<String> contextId) {

        Optional<Timestamp> lower = Transform.toTimestamp(start);
        Optional<Timestamp> upper = Transform.toTimestamp(end);
        Optional<Duration> step = Transform.toDuration(resolution);
        Context context = contextId.isPresent() ? new Context(contextId.get()) : Context.DEFAULT_CONTEXT;

        LOG.debug(
                "Retrieving measurements for resource {}, from {} to {} w/ resolution {} and w/ report {}",
                resource,
                lower,
                upper,
                step,
                descriptorDTO);

        ResultDescriptor rDescriptor = Transform.resultDescriptor(descriptorDTO);

        return Transform.measurementDTOs(m_repository.select(context, resource, lower, upper, rDescriptor, step));
    }

    @GET
    @Path("/{report}/{resource}")
    @Timed
    public Collection<Collection<MeasurementDTO>> getMeasurements(
            @PathParam("report") String report,
            @PathParam("resource") Resource resource,
            @QueryParam("start") Optional<TimestampParam> start,
            @QueryParam("end") Optional<TimestampParam> end,
            @QueryParam("resolution") Optional<DurationParam> resolution,
            @QueryParam("context") Optional<String> contextId) {

        ResultDescriptorDTO descriptorDTO = m_reports.get(report);

        // Report not found; 404
        if (descriptorDTO == null) {
            return null;
        }

        return getMeasurements(descriptorDTO, resource, start, end, resolution, contextId);
    }

}
