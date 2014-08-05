package org.opennms.newts.search.cassandra;


import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.Version;
import org.opennms.newts.api.Sample;
import org.opennms.newts.api.search.Indexer;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;


public class CassandraLuceneIndexer implements Indexer {

    private static final Version VERSION = Version.LUCENE_4_9;

    private IndexWriter m_writer;
    private final Cache<String, CacheEntry> m_cache;

    public CassandraLuceneIndexer(Directory directory) throws IOException {
        IndexWriterConfig config = new IndexWriterConfig(VERSION, new StandardAnalyzer(VERSION));
        m_writer = new IndexWriter(directory, config);  // FIXME: directory non-null
        m_cache = CacheBuilder.newBuilder().maximumSize(/* FIXME */1000000 /* FIXME */).build();
    }

    @Override
    public void update(Collection<Sample> samples) throws IOException {

        for (Sample sample : samples) {
            CacheEntry ref = getOrCreateCacheEntry(sample);

            // If this sample contains attributes (they are optional).
            if (sample.getResource().getAttributes().isPresent()) {
                Map<String, String> attrs = sample.getResource().getAttributes().get();
                
                // If the hash of this sample's attributes doesn't match our cached state
                // (i.e. they have changed since we saw them last).
                if (!ref.attributesMatch(attrs)) {

                    // Build a Lucene Document.
                    Document doc = new Document();
                    doc.add(new StringField("_resource", sample.getResource().getId(), Field.Store.YES));
                    doc.add(new StringField("_all", sample.getResource().getId(), Field.Store.NO));

                    for (Map.Entry<String, String> attr : attrs.entrySet()) {
                        doc.add(new StringField(attr.getKey(), attr.getValue(), Field.Store.YES));
                        doc.add(new StringField("_all", attr.getValue(), Field.Store.NO));
                    }

                    // Update (search, delete, replace).
                    m_writer.updateDocument(new Term("_resource", sample.getResource().getId()), doc);

                }

            }
        }

    }

    void close() throws IOException {
        m_writer.close();
    }

    private CacheEntry getOrCreateCacheEntry(Sample sample) {
        CacheEntry cacheEntry = m_cache.getIfPresent(sample.getResource().getId());
        if (cacheEntry == null) {
            cacheEntry = new CacheEntry();
            cacheEntry.setAttributesHash(-1);
            m_cache.put(sample.getResource().getId(), cacheEntry);
        }
        return cacheEntry;
    }

    private static class CacheEntry {

        private int m_attributesHash;

        private boolean attributesMatch(Map<String, String> other) {
            return hash(other) == getAttributesHash();
        }

        public int getAttributesHash() {
            return m_attributesHash;
        }

        public void setAttributesHash(int attributesHash) {
            m_attributesHash = attributesHash;
        }

    }

    private static int hash(Map<String, String> attributes) {
        return Objects.hashCode(attributes);
    }

}
