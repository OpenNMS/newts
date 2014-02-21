package org.opennms.newts.persistence.cassandra;


import javax.xml.bind.annotation.*;
import org.opennms.newts.api.MetricType;


@XmlAccessorType(XmlAccessType.FIELD)
public class XMLSample {

    @XmlElement(name = "Timestamp")
    private long m_timestamp;

    @XmlElement(name = "Name")
    private String m_name;

    @XmlElement(name = "Type")
    private MetricType m_type;

    @XmlElement(name = "Value")
    private Double m_value;

    public long getTimestamp() {
        return m_timestamp;
    }

    public void setTimestamp(long timestamp) {
        m_timestamp = timestamp;
    }

    public String getName() {
        return m_name;
    }

    public void setName(String name) {
        m_name = name;
    }

    public MetricType getType() {
        return m_type;
    }

    public void setType(MetricType type) {
        m_type = type;
    }

    public Double getValue() {
        return m_value;
    }

    public void setValue(Double value) {
        m_value = value;
    }

}
