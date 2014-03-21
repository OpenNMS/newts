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

import org.opennms.newts.api.Results;
import org.opennms.newts.api.Sample;
import org.opennms.newts.api.SampleRepository;
import org.opennms.newts.api.Timestamp;

import com.google.common.base.Optional;
import com.yammer.metrics.annotation.Timed;


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
        m_sampleRepository.insert(Transform.sampleDTOs(samples));
        return Response.status(Response.Status.CREATED).build();
    }

    @GET
    @Timed
    @Path("/{resource}")
    public Results<Sample> getSamples(@PathParam("resource") String resource,
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

        return m_sampleRepository.select(resource, lower, upper);

    }

}
