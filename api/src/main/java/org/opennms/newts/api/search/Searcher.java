package org.opennms.newts.api.search;


public interface Searcher {
    public SearchResults search(String queryString);
}
