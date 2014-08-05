package org.opennms.newts.search.cassandra;


import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.Version;
import org.opennms.newts.api.Resource;
import org.opennms.newts.api.search.QueryParseException;
import org.opennms.newts.api.search.Searcher;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;


public class CassandraLuceneSearcher implements Searcher {

    private static final Version VERSION = Version.LUCENE_4_9;  // TODO: DRY; make common?

    private IndexSearcher m_searcher;

    public CassandraLuceneSearcher(Directory directory) throws IOException {
        IndexReader reader = DirectoryReader.open(directory); // FIXME: directory non-null
        m_searcher = new IndexSearcher(reader);
    }

    @Override
    public Collection<Resource> search(String searchString) throws IOException, QueryParseException {

        Query query;
        TopScoreDocCollector collector = TopScoreDocCollector.create(10, true);
        List<Resource> resources = Lists.newArrayList();

        try {
            query = new QueryParser(VERSION, "_all", new StandardAnalyzer(VERSION)).parse(searchString);
        }
        catch (ParseException e) {
            throw new QueryParseException(e);
        }

        m_searcher.search(query, collector);

        for (ScoreDoc hit : collector.topDocs().scoreDocs) {
            Document doc = m_searcher.doc(hit.doc);
            Map<String, String> attributes = Maps.newHashMap();
            resources.add(new Resource(doc.get("_resource"), Optional.of(attributes)));

            for (IndexableField f : doc.getFields()) {
                if (f.name().startsWith("_")) continue;
                attributes.put(f.name(), f.stringValue());
            }

        }

        return resources;
    }

}
