package org.opennms.newts.rest.indexing;


import java.util.Collection;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;


@Path("/indices/resources")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ResourceIndexResource {

    // FIXME: A provider might be a better option for this. See:
    // http://codahale.com/what-makes-jersey-interesting-injection-providers/

    @GET
    @Path("/{search: .*}")
    public Collection<String> search(@PathParam("search") PathElements search) {
        throw new UnsupportedOperationException("stub");
    }

}
