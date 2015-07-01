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


import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Map;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.opennms.newts.api.Duration;
import org.opennms.newts.api.Measurement;
import org.opennms.newts.api.Resource;
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

        final Results<Measurement> results = new Results<>();

        when(
                m_repository.select(
                        eq(new Resource("localhost")),
                        eq(Optional.of(Timestamp.fromEpochSeconds(900000000))),
                        eq(Optional.of(Timestamp.fromEpochSeconds(900003600))),
                        any(ResultDescriptor.class),
                        eq(Duration.seconds(900)))
        ).thenReturn(results);

        // Reference the report by name
        assertThat(
                m_resource.getMeasurements(
                        "temps",
                        new Resource("localhost"),
                        Optional.of(new TimestampParam("1998-07-09T11:00:00-0500")),
                        Optional.of(new TimestampParam("1998-07-09T12:00:00-0500")),
                        Optional.of(new DurationParam("15m"))),
                CoreMatchers.instanceOf(Collection.class));

        // Include the report in the request
        assertThat(
                m_resource.getMeasurements(
                        getResultDescriptorDTO(),
                        new Resource("localhost"),
                        Optional.of(new TimestampParam("1998-07-09T11:00:00-0500")),
                        Optional.of(new TimestampParam("1998-07-09T12:00:00-0500")),
                        Optional.of(new DurationParam("15m"))),
                CoreMatchers.instanceOf(Collection.class));
    }

    private static ResultDescriptorDTO getResultDescriptorDTO() throws JsonProcessingException, IOException {
        InputStream json = MeasurementsResourceTest.class.getResourceAsStream(JSON_SAMPLE);
        return new ObjectMapper().reader(ResultDescriptorDTO.class).readValue(json);
    }

}
