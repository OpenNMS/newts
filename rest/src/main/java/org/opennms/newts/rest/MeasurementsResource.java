package org.opennms.newts.rest;


import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.opennms.newts.api.SampleRepository;

import com.yammer.metrics.annotation.Timed;


@Path("/measurements")
@Produces(MediaType.APPLICATION_JSON)
public class MeasurementsResource {

    @SuppressWarnings("unused") private final SampleRepository m_sampleRepository;

    public MeasurementsResource(SampleRepository sampleRepository) {
        m_sampleRepository = checkNotNull(sampleRepository, "sample repository");
    }

    @GET
    @Timed
    public Collection<MeasurementDTO> getMeasurements() {
        return null;
    }

}
