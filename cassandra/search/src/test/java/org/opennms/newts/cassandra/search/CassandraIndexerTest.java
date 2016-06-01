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
package org.opennms.newts.cassandra.search;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.opennms.newts.api.Gauge;
import org.opennms.newts.api.MetricType;
import org.opennms.newts.api.Resource;
import org.opennms.newts.api.Sample;
import org.opennms.newts.api.Timestamp;
import org.opennms.newts.cassandra.CassandraSession;
import org.opennms.newts.cassandra.ContextConfigurations;

import com.codahale.metrics.MetricRegistry;
import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.RegularStatement;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Statement;
import com.google.common.collect.Lists;

import static org.mockito.Mockito.*;

import java.util.List;

public class CassandraIndexerTest {

    @Test
    public void insertStatementsAreDeduplicatedWhenIndexingManySamples() {
        CassandraSession session = mock(CassandraSession.class);
        ArgumentCaptor<Statement> statementCaptor = ArgumentCaptor.forClass(Statement.class);
        when(session.executeAsync(statementCaptor.capture())).thenReturn(mock(ResultSetFuture.class));

        PreparedStatement statement = mock(PreparedStatement.class);
        BoundStatement boundStatement = mock(BoundStatement.class);
        when(session.prepare(any(RegularStatement.class))).thenReturn(statement);
        when(statement.bind()).thenReturn(boundStatement);
        when(boundStatement.setString(any(String.class), any(String.class))).thenReturn(boundStatement);

        CassandraIndexingOptions options = new CassandraIndexingOptions.Builder()
                .withHierarchicalIndexing(true)
                // Limit the batch size so we can accurately count the number of statements
                .withMaxBatchSize(1).build();

        MetricRegistry registry = new MetricRegistry();
        GuavaResourceMetadataCache cache = new GuavaResourceMetadataCache(2048, registry);
        CassandraIndexer indexer = new CassandraIndexer(session, 0, cache, registry, options,
                new EscapableResourceIdSplitter(), new ContextConfigurations());

        Resource r = new Resource("snmp:1589:vmware5Cpu:2:vmware5Cpu");
        List<Sample> samples = Lists.newArrayList();
        samples.add(new Sample(Timestamp.now(), r, "CpuCostopSum", MetricType.GAUGE, new Gauge(0)));
        samples.add(new Sample(Timestamp.now(), r, "CpuIdleSum", MetricType.GAUGE, new Gauge(19299.0)));  
        samples.add(new Sample(Timestamp.now(), r, "CpuMaxLdSum", MetricType.GAUGE, new Gauge(0)));  
        samples.add(new Sample(Timestamp.now(), r, "CpuOverlapSum", MetricType.GAUGE, new Gauge(5.0)));
        samples.add(new Sample(Timestamp.now(), r, "CpuRdySum", MetricType.GAUGE, new Gauge(41.0)));  
        samples.add(new Sample(Timestamp.now(), r, "CpuRunSum", MetricType.GAUGE, new Gauge(619.0)));  
        samples.add(new Sample(Timestamp.now(), r, "CpuSpwaitSum", MetricType.GAUGE, new Gauge(0))); 
        samples.add(new Sample(Timestamp.now(), r, "CpuSystemSum", MetricType.GAUGE, new Gauge(0))); 
        samples.add(new Sample(Timestamp.now(), r, "CpuUsagemhzAvg", MetricType.GAUGE, new Gauge(32.0)));
        samples.add(new Sample(Timestamp.now(), r, "CpuUsedSum", MetricType.GAUGE, new Gauge(299.0)));
        samples.add(new Sample(Timestamp.now(), r, "CpuWaitSum", MetricType.GAUGE, new Gauge(19343)));

        // Index the collection of samples
        indexer.update(samples);

        // Verify the number of exectuteAsync calls
        verify(session, times(20)).executeAsync(any(Statement.class));
    }
}
