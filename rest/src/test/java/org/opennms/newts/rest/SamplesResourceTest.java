package org.opennms.newts.rest;


import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyCollectionOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;

import javax.ws.rs.core.Response;

import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.opennms.newts.api.Results;
import org.opennms.newts.api.Sample;
import org.opennms.newts.api.SampleRepository;
import org.opennms.newts.api.Timestamp;

import com.google.common.base.Optional;


public class SamplesResourceTest {

    private final SampleRepository m_repository = mock(SampleRepository.class);
    private final SamplesResource m_resource = new SamplesResource(m_repository);

    @Test
    public void testWriteSamples() {

        Response response = m_resource.writeSamples(Collections.<SampleDTO> emptyList());

        assertThat(response.getStatus(), equalTo(Response.Status.CREATED.getStatusCode()));

        verify(m_repository).insert(anyCollectionOf(Sample.class));

    }

    @Test
    public void testGetSamples() {

        @SuppressWarnings("unchecked")
        final Results<Sample> results = mock(Results.class);

        when(
                m_repository.select(
                        eq("localhost"),
                        eq(Optional.of(Timestamp.fromEpochSeconds(900000000))),
                        eq(Optional.of(Timestamp.fromEpochSeconds(900003600))))
        ).thenReturn(results);

        assertThat(
                m_resource.getSamples(
                        "localhost",
                        Optional.of("1998-07-09T11:00:00-0500"),
                        Optional.of("1998-07-09T12:00:00-0500")),
                CoreMatchers.is(results));

    }

}
