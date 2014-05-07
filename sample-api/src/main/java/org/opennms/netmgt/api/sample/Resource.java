package org.opennms.netmgt.api.sample;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="resource")
@XmlAccessorType(XmlAccessType.NONE)
public class Resource implements Comparable<Resource>, Serializable {
    private static final long serialVersionUID = 3L;

    @XmlElement(name="agent")
    private final Agent m_agent;
    
    @XmlAttribute(name="name")
    private final String m_name;
    
    @XmlAttribute(name="label")
    private final String m_label;
    
    @XmlAttribute(name="type")
    private final String m_type;
    
    @XmlElementWrapper(name="attributes")
    @XmlElement(name="attribute")
    private final Map<String, String> m_attributes = new HashMap<String, String>();

    public Resource() {
        m_agent = null;
        m_name = null;
        m_label = null;
        m_type = null;
    }

    public Resource(final Agent agent, final String type, final String name, final String label) {
        m_agent = agent;
        m_name = name;
        m_label = label;
        m_type = type;
    }

    public String getIdentifier() {
        return String.format("%s|%s|%s", m_agent.getId(), m_type, m_name);
    }

    public String getType() {
        return m_type;
    }

    public String getName() {
        return m_name;
    }

    public String getLabel() {
        return m_label;
    }

    public Agent getAgent() {
        return m_agent;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (!(other instanceof Resource)) {
            return false;
        }
        return getIdentifier().equals(((Resource)other).getIdentifier());
    }

    @Override
    public int hashCode() {
        return getIdentifier().hashCode();
    }

    @Override
    public int compareTo(Resource other) {
        return getIdentifier().compareTo(other.getIdentifier());
    }

    public void setAttribute(String attrName, String attrValue) {
        m_attributes.put(attrName, attrValue);
    }

    public String getAttribute(String attrName) {
        return m_attributes.get(attrName);
    }

    public Map<String, String> getAttributes() {
        return m_attributes;
    }

    @Override
    public String toString() {
        return getIdentifier();
    }
}
