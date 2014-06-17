package org.opennms.minion.controller.core.internal;

import java.util.Date;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.opennms.minion.controller.api.IMinionStatus;

@XmlRootElement(name="minion-status")
@XmlAccessorType(XmlAccessType.NONE)
public class MinionStatusImpl implements IMinionStatus {

    @XmlAttribute(name="id")
    private String m_id;

    @XmlElement(name="location")
    private String m_location;

    @XmlElement(name="status")
    private String m_status;

    @XmlElement(name="date")
    private Date m_date;

    @Override
    public String getId() {
        return m_id;
    }

    public void setId(final String id) {
        m_id = id;
    }

    @Override
    public String getLocation() {
        return m_location;
    }

    public void setLocation(final String location) {
        m_location = location;
    }

    @Override
    public String getStatus() {
        return m_status;
    }

    public void setStatus(final String status) {
        m_status = status;
    }

    @Override
    public Date getDate() {
        return m_date;
    }

    public void setDate(final Date date) {
        m_date = date;
    }

}
