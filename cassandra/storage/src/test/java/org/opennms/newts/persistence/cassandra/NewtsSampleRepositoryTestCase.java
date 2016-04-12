/*
 * Copyright 2016, The OpenNMS Group
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
package org.opennms.newts.persistence.cassandra;

import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.junit.Rule;
import org.opennms.newts.api.SampleProcessorService;
import org.opennms.newts.cassandra.ContextConfigurations;
import org.opennms.newts.cassandra.NewtsInstance;

import com.codahale.metrics.MetricRegistry;

public class NewtsSampleRepositoryTestCase {

    @Rule
    public NewtsInstance newtsInstance = new NewtsInstance();
    
    public static final int CASSANDRA_TTL = 86400;

    protected CassandraSampleRepository m_repository;
    protected ContextConfigurations m_contextConfigurations = new ContextConfigurations();

    @Before
    public void setUp() throws Exception {
        m_repository = new CassandraSampleRepository(
                newtsInstance.getCassandraSession(),
                CASSANDRA_TTL,
                new MetricRegistry(),
                mock(SampleProcessorService.class),
                m_contextConfigurations);
    }

    public CassandraSampleRepository getRepository() {
        return m_repository;
    }
}
