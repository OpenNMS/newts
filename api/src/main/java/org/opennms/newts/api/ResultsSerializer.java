package org.opennms.newts.api;


import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;


public class ResultsSerializer extends JsonSerializer<Results<Element<?>>> {

    /** {@inheritDoc} */
    @Override
    public void serialize(Results<Element<?>> value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {

        jgen.writeStartArray();

        for (Results.Row<Element<?>> row : value) {
            jgen.writeStartArray();

            for (Element<?> elem : row.getElements()) {
                jgen.writeStartObject();
                provider.defaultSerializeField(elem.getName(), elem, jgen);
                jgen.writeEndObject();
            }

            jgen.writeEndArray();
        }

        jgen.writeEndArray();

    }

}
