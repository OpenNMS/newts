package org.opennms.newts.persistence.cassandra;


import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import org.opennms.newts.api.Duration;
import org.opennms.newts.api.Measurement;
import org.opennms.newts.api.MetricType;
import org.opennms.newts.api.Results;
import org.opennms.newts.api.Sample;
import org.opennms.newts.api.Timestamp;
import org.opennms.newts.api.ValueType;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;


@XmlRootElement(name = "TestCase")
@XmlAccessorType(XmlAccessType.FIELD)
public class XMLTestSpecification {

    public static final MetricType DEFAULT_SAMPLE_TYPE = MetricType.GAUGE;

    @XmlElement(name = "Resource")
    private String m_resource;

    @XmlElement(name = "Start")
    private long m_start;

    @XmlElement(name = "End")
    private long m_end;

    @XmlElement(name = "Interval")
    private long m_interval;

    @XmlElement(name = "Heartbeat")
    private long m_heartbeat;

    @XmlElement(name = "Resolution")
    private long m_resolution;

    @XmlElementWrapper(name = "Datasources")
    @XmlElement(name = "Datasource")
    private List<XMLDatasource> m_datasources;

    @XmlElementWrapper(name = "Exports")
    @XmlElement(name = "Export")
    private List<String> m_exports;
    
    @XmlElementWrapper(name = "TestData")
    @XmlElement(name = "Element")
    private List<XMLElement> m_testData;

    @XmlElementWrapper(name = "Expected")
    @XmlElement(name = "Element")
    private List<XMLElement> m_expected;

    public String getResource() {
        return m_resource;
    }

    public Timestamp getStart() {
        return Timestamp.fromEpochSeconds(m_start);
    }

    public Timestamp getEnd() {
        return Timestamp.fromEpochSeconds(m_end);
    }

    public Duration getInterval() {
        return Duration.seconds(m_interval);
    }

    public Duration getHeartbeat() {
        return Duration.seconds(m_heartbeat);
    }

    public Duration getResolution() {
        return Duration.seconds(m_resolution);
    }

    public List<XMLDatasource> getDatasources() {
        return (m_datasources != null) ? m_datasources : Collections.<XMLDatasource>emptyList();
    }

    public Set<String> getExports() {
        return (m_exports != null) ? Sets.newHashSet(m_exports) : Collections.<String>emptySet();
    }

    public String[] getMetrics() {

        Function<XMLElement, String> toMetricName = new Function<XMLElement, String>() {

            @Override
            public String apply(XMLElement input) {
                return input.getName();
            }
        };

        return Sets.newHashSet(Iterables.transform(m_testData, toMetricName)).toArray(new String[0]);
    }

    public Results<Sample> getTestDataAsSamples() {
        return getTestDataAsSamples(DEFAULT_SAMPLE_TYPE);
    }

    public Results<Sample> getTestDataAsSamples(final MetricType type) {

        Function<XMLElement, Sample> toSample = new Function<XMLElement, Sample>() {

            @Override
            public Sample apply(XMLElement input) {
                return new Sample(
                        Timestamp.fromEpochSeconds(input.getTimestamp()),
                        getResource(),
                        input.getName(),
                        type,
                        ValueType.compose(input.getValue(), type));
            }
        };

        Results<Sample> r = new Results<>();

        for (Sample s : Iterables.transform(m_testData, toSample)) {
            r.addElement(s);
        }

        return r;
    }

    public Results<Measurement> getTestDataAsMeasurements() {

        Function<XMLElement, Measurement> toSample = new Function<XMLElement, Measurement>() {

            @Override
            public Measurement apply(XMLElement input) {
                return new Measurement(
                        Timestamp.fromEpochSeconds(input.getTimestamp()),
                        getResource(),
                        input.getName(),
                        input.getValue());
            }
        };

        Results<Measurement> r = new Results<>();

        for (Measurement m : Iterables.transform(m_testData, toSample)) {
            r.addElement(m);
        }

        return r;
    }

    public Results<Measurement> getExpected() {

        Function<XMLElement, Measurement> toMeasurement = new Function<XMLElement, Measurement>() {

            @Override
            public Measurement apply(XMLElement input) {
                return new Measurement(
                        Timestamp.fromEpochSeconds(input.getTimestamp()),
                        getResource(),
                        input.getName(),
                        input.getValue());
            }
        };

        Results<Measurement> r = new Results<>();

        for (Measurement m : Iterables.transform(m_expected, toMeasurement)) {
            r.addElement(m);
        }

        return r;
    }

}
