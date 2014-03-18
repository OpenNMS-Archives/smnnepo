package org.opennms.netmgt.sampler.config.snmp;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import org.opennms.netmgt.config.api.collection.IRrd;

public class Rrd implements IRrd {

    @XmlAttribute(name="step")
    private int m_step;

    @XmlElement(name="rra")
    private String[] m_rras;

    @Override
    public int getStep() {
        return m_step;
    }

    @Override
    public String[] getRras() {
        return m_rras;
    }

}
