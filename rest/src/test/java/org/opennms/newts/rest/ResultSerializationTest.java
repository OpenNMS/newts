package org.opennms.newts.rest;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.opennms.newts.api.MetricType.COUNTER;

import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opennms.newts.api.Measurement;
import org.opennms.newts.api.Resource;
import org.opennms.newts.api.Results;
import org.opennms.newts.api.Sample;
import org.opennms.newts.api.Timestamp;
import org.opennms.newts.api.ValueType;
import org.opennms.newts.api.search.SearchResults;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class ResultSerializationTest {

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testMeasurements() throws JsonProcessingException {
        
        Results<Measurement> data = new Results<>();
        data.addElement(new Measurement(Timestamp.fromEpochSeconds(900000000), new Resource("localhost"), "ifInOctets", 5000));
        data.addElement(new Measurement(Timestamp.fromEpochSeconds(900000000), new Resource("localhost"), "ifOutOctets", 6000));
        data.addElement(new Measurement(Timestamp.fromEpochSeconds(900000300), new Resource("localhost"), "ifInOctets", 6000));
        data.addElement(new Measurement(Timestamp.fromEpochSeconds(900000300), new Resource("localhost"), "ifOutOctets", 7000));

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

        assertThat(new ObjectMapper().writeValueAsString(Transform.measurementDTOs(data)), is(normalize(json)));
        
    }

    @Test
    public void testSamples() throws JsonProcessingException {

        // Use the optional attributes map at least once.
        Map<String, String> attributes = Maps.newHashMap();
        attributes.put("units", "bytes");

        Results<Sample> data = new Results<>();
        data.addElement(new Sample(
                Timestamp.fromEpochSeconds(900000000),
                new Resource("localhost"),
                "ifInOctets",
                COUNTER,
                ValueType.compose(5000, COUNTER)));
        data.addElement(new Sample(
                Timestamp.fromEpochSeconds(900000000),
                new Resource("localhost"),
                "ifOutOctets",
                COUNTER,
                ValueType.compose(6000, COUNTER),
                attributes));
        data.addElement(new Sample(
                Timestamp.fromEpochSeconds(900000300),
                new Resource("localhost"),
                "ifInOctets",
                COUNTER,
                ValueType.compose(6000, COUNTER)));
        data.addElement(new Sample(
                Timestamp.fromEpochSeconds(900000300),
                new Resource("localhost"),
                "ifOutOctets",
                COUNTER,
                ValueType.compose(7000, COUNTER)));

        String json =  "["
                + "  ["
                + "    {"
                + "      \"name\": \"ifOutOctets\","
                + "      \"timestamp\":900000000000,"
                + "      \"type\":\"COUNTER\","
                + "      \"value\":6000,"
                + "      \"attributes\":{\"units\":\"bytes\"}"
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

        assertThat(new ObjectMapper().writeValueAsString(Transform.sampleDTOs(data)), is(normalize(json)));

    }

    @Test
    public void testSearchResults() throws JsonProcessingException {
        SearchResults results = new SearchResults();
        results.addResult(new Resource("localhost"), Lists.newArrayList("beer", "sausages"));

        String json = "["
                + "  {"
                + "    \"resource\": {"
                + "      \"id\":\"localhost\","
                + "      \"attributes\":{}"
                + "    },"
                + "    \"metrics\":["
                + "       \"beer\","
                + "       \"sausages\""
                + "    ]"
                + "   }"
                + "]";

        assertThat(new ObjectMapper().writeValueAsString(Transform.searchResultDTOs(results)), is(normalize(json)));

    }

    private String normalize(String input) {
        return input.replaceAll("\\n", "").replaceAll("\\s", "");
    }

}
