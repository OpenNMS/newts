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
import org.opennms.newts.api.Results.Row;
import org.opennms.newts.api.SampleRepository;
import org.opennms.newts.api.Timestamp;
import org.opennms.newts.api.query.ResultDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Collections2;
import com.yammer.metrics.annotation.Timed;


@Path("/measurements")
@Produces(MediaType.APPLICATION_JSON)
public class MeasurementsResource {

    private static final Logger LOG = LoggerFactory.getLogger(MeasurementsResource.class);
    private static final Function<Measurement, MeasurementDTO> MEASUREMENT_TO_DTO;

    static {
        MEASUREMENT_TO_DTO = new Function<Measurement, MeasurementDTO>() {

            @Override
            public MeasurementDTO apply(Measurement input) {
                return new MeasurementDTO(
                        input.getTimestamp().asMillis(),
                        input.getResource(),
                        input.getName(),
                        input.getValue());
            }
        };
    }

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
            @QueryParam("start") Optional<Long> start,
            @QueryParam("end") Optional<Long> end,
            @QueryParam("resolution") Optional<Integer> resolution) {

        Optional<Timestamp> lower = getOptionalTimestamp(start);
        Optional<Timestamp> upper = getOptionalTimestamp(end);

        LOG.trace(
                "Retrieving matching measurements for report {}, resource {}, from {} to {}",
                report,
                resource,
                lower,
                upper);

        ResultDescriptorDTO descrDTO = m_reports.get(report);

        if (descrDTO == null) {
            return null;
        }

        ResultDescriptor rDescriptor = new ResultDescriptor(Duration.millis(descrDTO.getInterval()));

        for (ResultDescriptorDTO.Datasource ds : descrDTO.getDatasources()) {
            rDescriptor.datasource(ds.getLabel(), ds.getSource(), ds.getHeartbeat(), ds.getFunction());
        }

        rDescriptor.export(descrDTO.getExports());

        Results<Measurement> measurements = m_repository.select(
                resource,
                lower,
                upper,
                rDescriptor,
                Duration.millis(resolution.get()));

        return Collections2.transform(measurements.getRows(), new Function<Row<Measurement>, Collection<MeasurementDTO>>() {

            @Override
            public Collection<MeasurementDTO> apply(Row<Measurement> input) {
                return Collections2.transform(input.getElements(), MEASUREMENT_TO_DTO);
            }
        });
    }

    // FIXME: Copypasta; Duplicated in SamplesResource.java
    private Optional<Timestamp> getOptionalTimestamp(Optional<Long> value) {
        return value.isPresent() ? Optional.of(Timestamp.fromEpochMillis(value.get())) : Optional.<Timestamp> absent();
    }

}
