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
package org.opennms.newts.rest;


import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.opennms.newts.api.Context;
import org.opennms.newts.api.search.Query;
import org.opennms.newts.api.search.SearchResults;
import org.opennms.newts.api.search.Searcher;
import org.opennms.newts.api.search.query.ParseException;
import org.opennms.newts.api.search.query.QueryParser;

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
    public SearchResults search(@QueryParam("q") Optional<String> query, @QueryParam("context") Optional<String> contextId) {
        checkArgument(query.isPresent(), "missing required query parameter (q=<argument>)");
        QueryParser qp = new QueryParser();
        Query parsedQuery;
        try {
            parsedQuery = qp.parse(query.get());
        } catch (ParseException e) {
            throw new WebApplicationException(e, Response.status(Status.BAD_REQUEST).entity("Invalid query " + query.get()).build());
        }
        Context context = contextId.isPresent() ? new Context(contextId.get()) : Context.DEFAULT_CONTEXT;
        return m_searcher.search(context, parsedQuery);
    }

}
