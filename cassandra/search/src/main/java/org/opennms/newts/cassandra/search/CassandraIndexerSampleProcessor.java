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
package org.opennms.newts.cassandra.search;


import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;

import javax.inject.Inject;

import org.opennms.newts.api.Sample;
import org.opennms.newts.api.SampleProcessor;
import org.opennms.newts.cassandra.CassandraException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class CassandraIndexerSampleProcessor implements SampleProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(CassandraIndexerSampleProcessor.class);

    private final CassandraIndexer m_indexer;

    @Inject
    public CassandraIndexerSampleProcessor(CassandraIndexer indexer) {
        m_indexer = checkNotNull(indexer, "indexer argument");
    }

    @Override
    public void submit(Collection<Sample> samples) {
        try                          { m_indexer.update(samples);}
        catch (CassandraException e) { LOG.error("failed to index samples", e); }
    }

}
