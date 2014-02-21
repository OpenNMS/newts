package org.opennms.newts.persistence.cassandra;


import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.opennms.newts.api.Duration;
import org.opennms.newts.api.Gauge;
import org.opennms.newts.api.Measurement;
import org.opennms.newts.api.Results;
import org.opennms.newts.api.Sample;
import org.opennms.newts.api.Timestamp;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;


@XmlRootElement(name = "TestCase")
@XmlAccessorType(XmlAccessType.FIELD)
public class XMLTestCase {

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

    @XmlElementWrapper(name = "Samples")
    @XmlElement(name = "Sample")
    private List<XMLSample> m_samples;

    @XmlElementWrapper(name = "Measurements")
    @XmlElement(name = "Measurement")
    private List<XMLMeasurement> m_measurements;

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

    @XmlTransient
    private Function<XMLSample, String> m_toMetricName = new Function<XMLSample, String>() {

        @Override
        public String apply(XMLSample input) {
            return input.getName();
        }
    };

    public String[] getMetrics() {
        return Sets.newHashSet(Iterables.transform(m_samples, m_toMetricName)).toArray(new String[0]);
    }

    public Results<Sample> getSamples() {

        Function<XMLSample, Sample> toSample = new Function<XMLSample, Sample>() {

            @Override
            public Sample apply(XMLSample input) {
                return new Sample(
                        Timestamp.fromEpochSeconds(input.getTimestamp()),
                        getResource(),
                        input.getName(),
                        input.getType(),
                        new Gauge(input.getValue()));
            }
        };

        Results<Sample> r = new Results<>();

        for (Sample s : Iterables.transform(m_samples, toSample)) {
            r.addElement(s);
        }

        return r;
    }

    public Results<Measurement> getMeasurements() {

        Function<XMLMeasurement, Measurement> toMeasurement = new Function<XMLMeasurement, Measurement>() {

            @Override
            public Measurement apply(XMLMeasurement input) {
                return new Measurement(
                        Timestamp.fromEpochSeconds(input.getTimestamp()),
                        getResource(),
                        input.getName(),
                        input.getValue());
            }
        };

        Results<Measurement> r = new Results<>();

        for (Measurement m : Iterables.transform(m_measurements, toMeasurement)) {
            r.addElement(m);
        }

        return r;
    }

}
