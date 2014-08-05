package org.opennms.newts.search.cassandra;


import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.util.Collection;

import javax.inject.Inject;

import org.opennms.newts.api.Sample;
import org.opennms.newts.api.SampleProcessor;
import org.opennms.newts.api.search.Indexer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class CassandraSearchSampleProcessor implements SampleProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(CassandraSearchSampleProcessor.class);

    private final Indexer m_indexer;

    @Inject
    public CassandraSearchSampleProcessor(Indexer indexer) {
        m_indexer = checkNotNull(indexer, "indexer argument");
    }

    @Override
    public void submit(Collection<Sample> samples) {
        try {
            m_indexer.update(samples);
        }
        catch (IOException e) {
            LOG.error("Failure performing search index update", e);
        }
    }

}
