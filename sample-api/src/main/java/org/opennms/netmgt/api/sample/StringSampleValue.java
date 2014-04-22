package org.opennms.netmgt.api.sample;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;

@XmlRootElement(name="sample-value")
public final class StringSampleValue implements Serializable {
    private static final long serialVersionUID = 1L;

    @XmlAttribute(name="value")
    private final String m_readableValue;

    @XmlValue
    private final String m_value;
    
    public StringSampleValue() {
        m_value = null;
        m_readableValue = null;
    }
    
    public StringSampleValue(final String value, final String readableValue) {
        m_value = value;
        m_readableValue = readableValue;
    }
    
    public StringSampleValue(final SampleValue<?> value) {
        m_value = SampleValue.toHex(value);
        m_readableValue = value.getValue().toString();
    }
    
    public String getValue() {
        return m_value;
    }
    
    @Override
    public String toString() {
        return m_value;
    }
}