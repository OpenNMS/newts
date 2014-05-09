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

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;


public class ResultDescriptorsResourceTest {

    private final static String JSON_SAMPLE = "/temperature.json";

    private final Map<String, ResultDescriptorDTO> m_reports = Maps.newHashMap();
    private final ResultDescriptorsResource m_resultsResource = new ResultDescriptorsResource(m_reports);

    @Before
    public void setUp() throws Exception {
        m_reports.put("temps", getResultDescriptorDTO());
    }

    @Test
    public void testGetReport() throws Exception {
        assertThat(
        		m_resultsResource.getReport("temps").toString(),
                CoreMatchers.equalTo(getResultDescriptorDTO().toString()));
    }

    private static ResultDescriptorDTO getResultDescriptorDTO() throws JsonProcessingException, IOException {
        InputStream json = ResultDescriptorsResourceTest.class.getResourceAsStream(JSON_SAMPLE);
        return new ObjectMapper().reader(ResultDescriptorDTO.class).readValue(json);
    }
}
