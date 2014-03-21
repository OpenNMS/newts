package org.opennms.newts.rest;


import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.opennms.newts.api.Duration;
import org.opennms.newts.api.Measurement;
import org.opennms.newts.api.Results;
import org.opennms.newts.api.SampleRepository;
import org.opennms.newts.api.Timestamp;
import org.opennms.newts.api.query.ResultDescriptor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.collect.Maps;


public class MeasurementsResourceTest {

    private final static String JSON_SAMPLE = "/temperature.json";

    private final SampleRepository m_repository = mock(SampleRepository.class);
    private final Map<String, ResultDescriptorDTO> m_reports = Maps.newHashMap();
    private final MeasurementsResource m_resource = new MeasurementsResource(m_repository, m_reports);

    @Before
    public void setUp() throws Exception {
        m_reports.put("temps", getResultDescriptorDTO());
    }

    @Test
    public void testGetMeasurements() throws Exception {

        @SuppressWarnings("unchecked")
        final Results<Measurement> results = mock(Results.class);

        when(
                m_repository.select(
                        eq("localhost"),
                        eq(Optional.of(Timestamp.fromEpochSeconds(900000000))),
                        eq(Optional.of(Timestamp.fromEpochSeconds(900003600))),
                        any(ResultDescriptor.class),
                        eq(Duration.seconds(900)))
        ).thenReturn(results);

        assertThat(
                m_resource.getMeasurements(
                        "temps",
                        "localhost",
                        Optional.of("1998-07-09T11:00:00-0500"),
                        Optional.of("1998-07-09T12:00:00-0500"),
                        Optional.of("15m")),
                CoreMatchers.is(results));
    }

    private static ResultDescriptorDTO getResultDescriptorDTO() throws JsonProcessingException, IOException {
        InputStream json = MeasurementsResourceTest.class.getResourceAsStream(JSON_SAMPLE);
        return new ObjectMapper().reader(ResultDescriptorDTO.class).readValue(json);
    }

}
