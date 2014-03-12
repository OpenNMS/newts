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
import org.opennms.newts.api.Results.Row;
import org.opennms.newts.api.Sample;
import org.opennms.newts.api.SampleRepository;
import org.opennms.newts.api.Timestamp;
import org.opennms.newts.api.ValueType;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Collections2;
import com.yammer.metrics.annotation.Timed;


@Path("/samples")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SamplesResource {

    private static final Function<Sample, SampleDTO> SAMPLE_TO_DTO;
    private static final Function<SampleDTO, Sample> DTO_TO_SAMPLE;

    static {
        SAMPLE_TO_DTO = new Function<Sample, SampleDTO>() {

            @Override
            public SampleDTO apply(Sample input) {
                return new SampleDTO(
                        input.getTimestamp().asMillis(),
                        input.getResource(),
                        input.getName(),
                        input.getType(),
                        input.getValue());
            }
        };

        DTO_TO_SAMPLE = new Function<SampleDTO, Sample>() {

            @Override
            public Sample apply(SampleDTO input) {
                return new Sample(
                        Timestamp.fromEpochMillis(input.getTimestamp()),
                        input.getResource(),
                        input.getName(),
                        input.getType(),
                        ValueType.compose(input.getValue(), input.getType()));
            }
        };
    }

    private final SampleRepository m_sampleRepository;

    public SamplesResource(SampleRepository sampleRepository) {
        m_sampleRepository = checkNotNull(sampleRepository, "sample repository");
    }

    @POST
    @Timed
    public Response writeSamples(Collection<SampleDTO> samples) {
        m_sampleRepository.insert(Collections2.transform(samples, DTO_TO_SAMPLE));
        return Response.status(Response.Status.CREATED).build();
    }

    @GET
    @Timed
    @Path("/{resource}")
    public Collection<Collection<SampleDTO>> getSamples(@PathParam("resource") String resource,
            @QueryParam("start") Optional<Long> start, @QueryParam("end") Optional<Long> end) {

        Optional<Timestamp> lower = getOptionalTimestamp(start);
        Optional<Timestamp> upper = getOptionalTimestamp(end);

        final Results<Sample> select = m_sampleRepository.select(resource, lower, upper);

        return Collections2.transform(select.getRows(), new Function<Row<Sample>, Collection<SampleDTO>>() {

            @Override
            public Collection<SampleDTO> apply(Row<Sample> input) {
                return Collections2.transform(input.getElements(), SAMPLE_TO_DTO);
            }
        });
    }

    private Optional<Timestamp> getOptionalTimestamp(Optional<Long> value) {
        return value.isPresent() ? Optional.of(Timestamp.fromEpochMillis(value.get())) : Optional.<Timestamp> absent();
    }

}
