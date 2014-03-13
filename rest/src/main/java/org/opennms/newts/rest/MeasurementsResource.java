package org.opennms.newts.rest;


import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.opennms.newts.api.Duration;
import org.opennms.newts.api.Measurement;
import org.opennms.newts.api.Results;
import org.opennms.newts.api.SampleRepository;
import org.opennms.newts.api.Timestamp;
import org.opennms.newts.api.query.ResultDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.yammer.metrics.annotation.Timed;


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

    @GET
    @Path("/{report}/{resource}")
    @Timed
    public Collection<Collection<MeasurementDTO>> getMeasurements(
            @PathParam("report") String report,
            @PathParam("resource") String resource,
            @QueryParam("start") Optional<Integer> start,
            @QueryParam("end") Optional<Integer> end,
            @QueryParam("resolution") Optional<Integer> resolution) {

        Optional<Timestamp> lower = Transform.fromOptionalSeconds(start);
        Optional<Timestamp> upper = Transform.fromOptionalSeconds(end);

        // TODO: Actually handle case of absent resolution.

        LOG.debug(
                "Retrieving measurements for report {}, resource {}, from {} to {} w/ resolution {}",
                report,
                resource,
                lower,
                upper,
                resolution);

        ResultDescriptorDTO descriptorDTO = m_reports.get(report);

        // Report not found; 404
        if (descriptorDTO == null) {
            return null;
        }

        ResultDescriptor rDescriptor = Transform.resultDescriptor(descriptorDTO);

        Results<Measurement> measurements = m_repository.select(
                resource,
                lower,
                upper,
                rDescriptor,
                Duration.seconds(resolution.get()));

        return Transform.measurements(measurements);
    }

}
