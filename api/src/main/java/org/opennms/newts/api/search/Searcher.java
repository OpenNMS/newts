package org.opennms.newts.api.search;

import org.opennms.newts.api.Context;

public interface Searcher {
    public SearchResults search(Query query);

    public SearchResults search(Query query, Context context);
}
