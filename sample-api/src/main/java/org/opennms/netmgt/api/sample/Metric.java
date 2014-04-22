package org.opennms.netmgt.api.sample;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="metric")
@XmlAccessorType(XmlAccessType.NONE)
public class Metric implements Serializable {
    private static final long serialVersionUID = 2L;

    @XmlAttribute(name="name")
    private final String m_name;

    @XmlAttribute(name="metric-type")
    private final MetricType m_type;
    
    @XmlAttribute(name="group")
    private final String m_group;

    public Metric() {
        m_name = null;
        m_type = null;
        m_group = null;
    }

    public Metric(final String name, final MetricType type, final String group) {
        m_name = name;
        m_type = type;
        m_group = group;
    }

    public String getName() {
        return m_name;
    }

    public MetricType getType() {
        return m_type;
    }

    public String getGroup() {
        return m_group;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Metric)) {
            return false;
        }

        Metric m = (Metric)obj;

        if (!(getName().equals(m.getName()) && getType().equals(m.getType()) && getGroup().equals(m.getGroup()))) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return String.format("%s%s%s", getName(), getType(), getGroup()).hashCode();
    }

    @Override
    public String toString() {
        return String.format(
                             "%s(name=%s, type=%s, group=%s)",
                             getClass().getSimpleName(),
                             getName(),
                             getType(),
                             getGroup());
    }

}
