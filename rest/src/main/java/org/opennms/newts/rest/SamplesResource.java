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

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.opennms.newts.api.Resource;
import org.opennms.newts.api.SampleRepository;
import org.opennms.newts.api.Timestamp;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Optional;


@Path("/samples")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SamplesResource {

    private final SampleRepository m_sampleRepository;

    public SamplesResource(SampleRepository sampleRepository) {
        m_sampleRepository = checkNotNull(sampleRepository, "sample repository");
    }

    @POST
    @Timed
    public Response writeSamples(Collection<SampleDTO> samples) {
        m_sampleRepository.insert(Transform.samples(samples));
        return Response.status(Response.Status.CREATED).build();
    }

    @GET
    @Timed
    @Path("/{resource}")
    public Collection<Collection<SampleDTO>> getSamples(@PathParam("resource") Resource resource,
            @QueryParam("start") Optional<String> start, @QueryParam("end") Optional<String> end) {

        /*
         * XXX: This resource method should use TimestampParam as the type for the start and end
         * arguments, but some of these custom parameters are causing Jersey fits, and so for
         * consistency sake we'll eschew them all for the time being.
         * 
         * Transform#timestampFromString uses TimestampParam to parse the String parameter, and so
         * appropriately excepts on validation failures (resulting in 400 Bad Request responses).
         */
        Optional<Timestamp> lower = Transform.timestampFromString(start);
        Optional<Timestamp> upper = Transform.timestampFromString(end);

        return Transform.sampleDTOs(m_sampleRepository.select(resource, lower, upper));

    }

}
