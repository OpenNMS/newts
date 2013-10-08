package org.opennms.newts.rest;


import java.io.IOException;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;

import spark.Request;
import spark.Response;
import spark.ResponseTransformerRoute;


public abstract class JsonTransformerRoute<T> extends ResponseTransformerRoute {

    protected final ObjectWriter m_jsonWriter = new ObjectMapper().writer();

    protected JsonTransformerRoute(String path) {
        super(path, "application/json");
    }

    @Override
    public String render(Object model) {

        try {
            return m_jsonWriter.writeValueAsString(model);
        }
        catch (IOException e) {
            throw new RuntimeException("Unable to marshal JSON response", e);
        }

    }

    @Override
    public abstract T handle(Request request, Response response);

}
