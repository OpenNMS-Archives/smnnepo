package org.opennms.netmgt.sampler.config.snmp;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

public class Rrd {

    @XmlAttribute(name="step")
    private int m_step;

    @XmlElement(name="rra")
    private String[] m_rras;

    public int getStep() {
        return m_step;
    }

    public String[] getRras() {
        return m_rras;
    }

}
