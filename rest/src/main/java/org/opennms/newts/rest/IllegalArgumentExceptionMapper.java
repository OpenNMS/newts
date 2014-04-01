package org.opennms.newts.rest;


import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;


public class IllegalArgumentExceptionMapper implements ExceptionMapper<IllegalArgumentException> {

    @Override
    public Response toResponse(IllegalArgumentException exception) {
        return Response.status(Response.Status.BAD_REQUEST).entity(exception.getLocalizedMessage()).build();
    }

}
