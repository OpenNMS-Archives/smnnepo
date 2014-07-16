package org.opennms.minion.impl;

import java.util.Date;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.opennms.minion.api.DateAdapter;
import org.opennms.minion.api.MinionException;
import org.opennms.minion.api.MinionStatusMessage;

@XmlRootElement(name="minion-status")
@XmlAccessorType(XmlAccessType.NONE)
public class MinionStatusMessageImpl extends AbstractMinionMessage implements MinionStatusMessage {
    @XmlAttribute(name="id")
    private String m_id;

    @XmlElement(name="location")
    private String m_location;

    @XmlElement(name="status")
    private String m_status;

    @XmlElement(name="date")
    @XmlJavaTypeAdapter(DateAdapter.class)
    private Date m_date;

    protected MinionStatusMessageImpl() {
        super();
    }
    
    public MinionStatusMessageImpl(final String id, final Integer version) throws MinionException {
        super(version);
        if (id == null) {
            throw new MinionException("ID must be defined!");
        }
        m_id = id;
    }

    @Override
    public String getId() {
        return m_id;
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

    @Override
    public String toString() {
        return "MinionStatusMessageImpl [id=" + m_id + ", location=" + m_location + ", status=" + m_status + ", date=" + m_date + ", properties=" + getProperties() + "]";
    }
}
