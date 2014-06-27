package org.opennms.minion.api;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;

@XmlRootElement(name="property")
@XmlAccessorType(XmlAccessType.NONE)
public final class AdaptedMap {
    @XmlAttribute(name="key")
    private String m_key;
    @XmlValue
    private String m_value;
    public AdaptedMap() {}
    public AdaptedMap(final String key, final String value) {
        m_key = key;
        m_value = value;
    }
    public String getKey() {
        return m_key;
    }
    public String getValue() {
        return m_value;
    }
    @Override
    public String toString() {
        return "AdaptedMap [key=" + m_key + ",value=" + m_value + "]";
    }
}