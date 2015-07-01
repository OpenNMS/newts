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


import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyCollectionOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Collections;

import javax.ws.rs.core.Response;

import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.opennms.newts.api.Context;
import org.opennms.newts.api.Resource;
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

        final Results<Sample> results = new Results<>();

        when(
                m_repository.select(
                        eq(Context.DEFAULT_CONTEXT),
                        eq(new Resource("localhost")),
                        eq(Optional.of(Timestamp.fromEpochSeconds(900000000))),
                        eq(Optional.of(Timestamp.fromEpochSeconds(900003600))))
        ).thenReturn(results);

        assertThat(
                m_resource.getSamples(
                        new Resource("localhost"),
                        Optional.of(new TimestampParam("1998-07-09T11:00:00-0500")),
                        Optional.of(new TimestampParam("1998-07-09T12:00:00-0500"))),
                CoreMatchers.instanceOf(Collection.class));

    }

}
