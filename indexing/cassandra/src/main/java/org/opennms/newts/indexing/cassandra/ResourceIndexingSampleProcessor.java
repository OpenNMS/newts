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
package org.opennms.newts.indexing.cassandra;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;

import javax.inject.Inject;

import org.opennms.newts.api.Sample;
import org.opennms.newts.api.SampleProcessor;
import org.opennms.newts.api.indexing.ResourceIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;


public class ResourceIndexingSampleProcessor implements SampleProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(ResourceIndexingSampleProcessor.class);

    private final ResourceIndex m_index;

    @Inject
    public ResourceIndexingSampleProcessor(ResourceIndex resourceIndex) {
        m_index = checkNotNull(resourceIndex, "resource index argument");
        LOG.warn("*** EXPERIMENTAL *** Created new sample processor for resource indexing");
    }

    @Override
    public void submit(Collection<Sample> samples) {
        if (LOG.isTraceEnabled()) LOG.trace("{} samples submitted", samples.size());
        m_index.index(collate(samples));
    }

    private Multimap<String, String> collate(Collection<Sample> samples) {
        Multimap<String, String> metrics = HashMultimap.create();

        for (Sample s : samples) {
            metrics.put(s.getResource().getId(), s.getName());
        }

        return metrics;
    }

}
