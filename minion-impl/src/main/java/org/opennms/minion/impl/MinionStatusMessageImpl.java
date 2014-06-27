package org.opennms.minion.impl;

import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.opennms.minion.api.DateAdapter;
import org.opennms.minion.api.MapAdapter;
import org.opennms.minion.api.MinionException;
import org.opennms.minion.api.MinionStatusMessage;

@XmlRootElement(name="minion-status")
@XmlAccessorType(XmlAccessType.NONE)
public class MinionStatusMessageImpl implements MinionStatusMessage {
    @XmlAttribute(name="id")
    private String m_id;

    @XmlAttribute(name="version")
    private Integer m_version;

    @XmlElement(name="location")
    private String m_location;

    @XmlElement(name="status")
    private String m_status;

    @XmlElement(name="date")
    @XmlJavaTypeAdapter(DateAdapter.class)
    private Date m_date;

    @XmlElement(name="properties")
    @XmlJavaTypeAdapter(MapAdapter.class)
    private Map<String,String> m_properties;

    protected MinionStatusMessageImpl() {
    }
    
    public MinionStatusMessageImpl(final String id, final Integer version) throws MinionException {
        if (id == null) {
            throw new MinionException("ID must be defined!");
        }
        if (version == null) {
            throw new MinionException("Message version must be defined!");
        }
        m_id = id;
        m_version = version;
    }

    @Override
    public String getId() {
        return m_id;
    }

    @Override
    public int getVersion() {
        return m_version.intValue();
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
    public Map<String,String> getProperties() {
        if (m_properties != null) {
            return Collections.unmodifiableMap(m_properties);
        } else {
            return Collections.emptyMap();
        }
    }

    public void addProperty(final String key, final String value) {
        assertPropertiesCreated();
        m_properties.put(key, value);
    }

    private void assertPropertiesCreated() {
        if (m_properties == null) {
            m_properties = new LinkedHashMap<String,String>();
        }
    }

    @Override
    public String toString() {
        return "MinionStatusMessageImpl [id=" + m_id + ", location=" + m_location + ", status=" + m_status + ", date=" + m_date + ", properties=" + m_properties + "]";
    }
}
