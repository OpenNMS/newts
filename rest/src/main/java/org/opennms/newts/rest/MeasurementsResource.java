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
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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
            @QueryParam("start") Optional<String> start,
            @QueryParam("end") Optional<String> end,
            @QueryParam("resolution") Optional<String> resolutionParam) {

        /*
         * XXX: This resource method should accept a DurationParam instance for the resolution query
         * parameter, and TimestampParam for start/end. However, for reasons I cannot not (yet) fathom,
         * Jersey bitches about a missing dependency at startup, and the resource is not loaded. 
         *
         * ERROR [2014-03-19 20:20:31,705] com.sun.jersey.spi.inject.Errors: The following errors and
         * warnings have been detected with resource and/or provider classes:
         *    SEVERE: Missing dependency for method public java.util.Collection org.opennms.newts.rest.MeasurementsResource.getMeasurements(java.lang.String,java.lang.String,com.google.common.base.Optional,com.google.common.base.Optional,org.opennms.newts.rest.DurationParam)
         * at parameter at index 4
         *
         * ETOOMUCHMAGIC
         *
         */
        Optional<Timestamp> lower = Transform.timestampFromString(start);
        Optional<Timestamp> upper = Transform.timestampFromString(end);

        if (!resolutionParam.isPresent()) {
            throw new WebApplicationException(
                    Response.status(Response.Status.BAD_REQUEST)
                            .entity("the 'resolution' query argument is mandatory (for the time being)")
                            .build());
        }

        DurationParam resolution = new DurationParam(resolutionParam.get());

        LOG.debug(
                "Retrieving measurements for resource {}, from {} to {} w/ resolution {} and w/ report {}",
                resource,
                lower,
                upper,
                resolution.get(),
                descriptorDTO);

        ResultDescriptor rDescriptor = Transform.resultDescriptor(descriptorDTO);

        return Transform.measurementDTOs(m_repository.select(resource, lower, upper, rDescriptor, resolution.get()));
    }

    @GET
    @Path("/{report}/{resource}")
    @Timed
    public Collection<Collection<MeasurementDTO>> getMeasurements(
            @PathParam("report") String report,
            @PathParam("resource") Resource resource,
            @QueryParam("start") Optional<String> start,
            @QueryParam("end") Optional<String> end,
            @QueryParam("resolution") Optional<String> resolutionParam) {

        ResultDescriptorDTO descriptorDTO = m_reports.get(report);

        // Report not found; 404
        if (descriptorDTO == null) {
            return null;
        }

        return getMeasurements(descriptorDTO, resource, start, end, resolutionParam);
    }

}
