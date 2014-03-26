package org.opennms.newts.api;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class SampleSerializer extends JsonSerializer<Sample> {

    @Override
    public void serialize(Sample value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
        jgen.writeStartObject();
        jgen.writeStringField("name", value.getName());
        jgen.writeNumberField("timestamp", value.getTimestamp().asSeconds());
        jgen.writeStringField("type", value.getType().toString());
        jgen.writeObjectField("value", value.getValue());
        jgen.writeEndObject();
    }

}
