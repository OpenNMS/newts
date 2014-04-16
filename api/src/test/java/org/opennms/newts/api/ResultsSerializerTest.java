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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.opennms.newts.api.MetricType.COUNTER;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;


public class ResultsSerializerTest {

    @Test
    public void testMeasurements() throws JsonProcessingException {

        Results<Measurement> testData = new Results<>();
        testData.addElement(new Measurement(Timestamp.fromEpochSeconds(900000000), "localhost", "ifInOctets", 5000));
        testData.addElement(new Measurement(Timestamp.fromEpochSeconds(900000000), "localhost", "ifOutOctets", 6000));
        testData.addElement(new Measurement(Timestamp.fromEpochSeconds(900000300), "localhost", "ifInOctets", 6000));
        testData.addElement(new Measurement(Timestamp.fromEpochSeconds(900000300), "localhost", "ifOutOctets", 7000));

        String json = "["
                + "  ["
                + "    {"
                + "      \"name\": \"ifOutOctets\","
                + "      \"timestamp\":900000000000,"
                + "      \"value\":6000.0"
                + "    },"
                + "    {"
                + "      \"name\": \"ifInOctets\","
                + "      \"timestamp\":900000000000,"
                + "      \"value\":5000.0"
                + "    }"
                + "  ],"
                + "  ["
                + "    {"
                + "      \"name\": \"ifOutOctets\","
                + "      \"timestamp\":900000300000,"
                + "      \"value\":7000.0"
                + "    },"
                + "    {"
                + "      \"name\": \"ifInOctets\","
                + "      \"timestamp\":900000300000,"
                + "      \"value\":6000.0"
                + "    }"
                + "  ]"
                + "]";

        ObjectMapper mapper = new ObjectMapper();

        assertThat(mapper.writeValueAsString(testData), is(normalize(json)));

    }

    @Test
    public void testSamples() throws JsonProcessingException {

        Results<Sample> testData = new Results<>();
        testData.addElement(new Sample(
                Timestamp.fromEpochSeconds(900000000),
                "localhost",
                "ifInOctets",
                COUNTER,
                ValueType.compose(5000, COUNTER)));
        testData.addElement(new Sample(
                Timestamp.fromEpochSeconds(900000000),
                "localhost",
                "ifOutOctets",
                COUNTER,
                ValueType.compose(6000, COUNTER)));
        testData.addElement(new Sample(
                Timestamp.fromEpochSeconds(900000300),
                "localhost",
                "ifInOctets",
                COUNTER,
                ValueType.compose(6000, COUNTER)));
        testData.addElement(new Sample(
                Timestamp.fromEpochSeconds(900000300),
                "localhost",
                "ifOutOctets",
                COUNTER,
                ValueType.compose(7000, COUNTER)));

        String json =  "["
                + "  ["
                + "    {"
                + "      \"name\": \"ifOutOctets\","
                + "      \"timestamp\":900000000000,"
                + "      \"type\":\"COUNTER\","
                + "      \"value\":6000"
                + "    },"
                + "    {"
                + "      \"name\": \"ifInOctets\","
                + "      \"timestamp\":900000000000,"
                + "      \"type\":\"COUNTER\","
                + "      \"value\":5000"
                + "    }"
                + "  ],"
                + "  ["
                + "    {"
                + "      \"name\": \"ifOutOctets\","
                + "      \"timestamp\":900000300000,"
                + "      \"type\":\"COUNTER\","
                + "      \"value\":7000"
                + "    },"
                + "    {"
                + "      \"name\": \"ifInOctets\","
                + "      \"timestamp\":900000300000,"
                + "      \"type\":\"COUNTER\","
                + "      \"value\":6000"
                + "    }"
                + "  ]"
                + "]";

        ObjectMapper mapper = new ObjectMapper();

        assertThat(mapper.writeValueAsString(testData), is(normalize(json)));

    }

    private String normalize(String input) {
        return input.replaceAll("\\n", "").replaceAll("\\s", "");
    }

}
