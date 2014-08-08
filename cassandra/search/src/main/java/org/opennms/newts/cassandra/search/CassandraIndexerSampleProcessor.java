package org.opennms.newts.cassandra.search;


import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;

import org.opennms.newts.api.Sample;
import org.opennms.newts.api.SampleProcessor;
import org.opennms.newts.cassandra.CassandraException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class CassandraIndexerSampleProcessor implements SampleProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(CassandraIndexerSampleProcessor.class);

    private final CassandraIndexer m_indexer;

    public CassandraIndexerSampleProcessor(CassandraIndexer indexer) {
        m_indexer = checkNotNull(indexer, "indexer argument");
    }

    @Override
    public void submit(Collection<Sample> samples) {
        try                          { m_indexer.update(samples);}
        catch (CassandraException e) { LOG.error("failed to index samples", e); }
    }

}
