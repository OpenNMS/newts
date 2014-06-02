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
        jgen.writeNumberField("timestamp", value.getTimestamp().asMillis());
        jgen.writeStringField("type", value.getType().toString());
        jgen.writeObjectField("value", value.getValue());

        // Since attributes is optional, be compact and omit from JSON output when unused.
        if (value.getAttributes() != null && !value.getAttributes().isEmpty()) {
            jgen.writeObjectField("attributes", value.getAttributes());
        }

        jgen.writeEndObject();
    }

}
