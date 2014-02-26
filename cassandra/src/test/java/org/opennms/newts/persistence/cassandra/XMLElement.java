package org.opennms.newts.persistence.cassandra;


import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;


@XmlAccessorType(XmlAccessType.FIELD)
public class XMLElement {

    @XmlElement(name = "Timestamp")
    private long m_timestamp;

    @XmlElement(name = "Name")
    private String m_name;

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

    public Double getValue() {
        return m_value;
    }

    public void setValue(Double value) {
        m_value = value;
    }

}
