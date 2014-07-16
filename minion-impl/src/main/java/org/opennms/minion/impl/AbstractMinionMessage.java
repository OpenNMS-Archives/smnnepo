package org.opennms.minion.impl;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.opennms.minion.api.MapAdapter;
import org.opennms.minion.api.MinionException;
import org.opennms.minion.api.MinionMessage;

public abstract class AbstractMinionMessage implements MinionMessage {
    @XmlAttribute(name="version")
    private Integer m_version;

    @XmlElement(name="properties")
    @XmlJavaTypeAdapter(MapAdapter.class)
    private Map<String,String> m_properties;

    protected AbstractMinionMessage() {
    }

    public AbstractMinionMessage(final Integer version) throws MinionException {
        if (version == null) {
            throw new MinionException("Message version must be defined!");
        }
        m_version = version;
    }

    @Override
    public int getVersion() {
        return m_version.intValue();
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
}
