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
package org.opennms.newts.api;


import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.opennms.newts.api.MetricType.GAUGE;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;


public class SampleProcessorServiceTest {

    private SampleProcessor m_processor = mock(SampleProcessor.class);
    private SampleProcessorService m_service;

    @Before
    public void setUp() throws Exception {
        m_service = new SampleProcessorService(1, Collections.singleton(m_processor));
    }

    @Test
    public void test() {

        Sample sample = new Sample(Timestamp.now(), new Resource("resource"), "metric", GAUGE, ValueType.compose(1, GAUGE));
        m_service.submit(Collections.singletonList(sample));

        try {
            m_service.shutdown();
            assertTrue("Executor shutdown failed", m_service.awaitShutdown(5, TimeUnit.SECONDS));
        }
        catch (InterruptedException e) {
            // Nada
        }

        verify(m_processor).submit(Collections.singletonList(sample));

    }

}