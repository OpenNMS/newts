package org.opennms.newts.api.search;


import java.io.IOException;
import java.util.Collection;

import org.opennms.newts.api.Resource;


public interface Searcher {

    /**
     * Performs a query of the search index using a <a
     * href="http://lucene.apache.org/core/2_9_4/queryparsersyntax.html">Lucene query string</a>.
     * Returns matching resources in score/relevance order.
     *
     * @param searchString
     *            search string
     * @return matching resources
     * @throws IOException
     * @throws QueryParseException
     */
    public Collection<Resource> search(String searchString) throws IOException, QueryParseException;

}
