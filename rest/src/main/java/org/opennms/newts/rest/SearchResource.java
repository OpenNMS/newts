package org.opennms.newts.rest;


import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.opennms.newts.api.search.SearchResults;
import org.opennms.newts.api.search.Searcher;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Optional;


@Path("/search")
@Produces(MediaType.APPLICATION_JSON)
public class SearchResource {

    private final Searcher m_searcher;

    public SearchResource(Searcher searcher) {
        m_searcher = checkNotNull(searcher, "searcher argument");
    }

    @GET
    @Timed
    public SearchResults search(@QueryParam("q") Optional<String> query) {
        checkArgument(query.isPresent(), "missing required query parameter (q=<argument>)");
        return m_searcher.search(query.get());
    }

}
