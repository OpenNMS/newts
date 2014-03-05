package org.opennms.newts.persistence.cassandra;


import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

import org.opennms.newts.api.query.StandardAggregationFunctions;


@XmlAccessorType(XmlAccessType.FIELD)
public class XMLDatasource {

    @XmlElement(name = "Label")
    private String m_label;

    @XmlElement(name = "Source")
    private String m_source;

    @XmlElement(name = "Function")
    private StandardAggregationFunctions m_function;

    public String getLabel() {
        return m_label;
    }

    public String getSource() {
        return m_source;
    }

    public StandardAggregationFunctions getFunction() {
        return m_function;
    }

}
