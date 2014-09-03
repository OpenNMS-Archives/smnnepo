package org.opennms.minion.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.opennms.minion.api.MinionContainerConfiguration;
import org.opennms.minion.api.MapAdapter;

@XmlRootElement(name="configuration")
@XmlAccessorType(XmlAccessType.NONE)
public class MinionContainerConfigurationImpl implements MinionContainerConfiguration {
    @XmlAttribute(name="pid")
    private String m_pid;

    @XmlElement(name="properties")
    @XmlJavaTypeAdapter(MapAdapter.class)
    private Map<String,String> m_properties = new LinkedHashMap<>();

    protected MinionContainerConfigurationImpl() {
    }

    public MinionContainerConfigurationImpl(final String pid) {
        m_pid = pid;
    }

    @Override
    public String getPid() {
        return m_pid;
    }

    @Override
    public boolean containsKey(final String key) {
        return m_properties.containsKey(key);
    }

    @Override
    public String getProperty(final String key) {
        return m_properties.get(key);
    }

    @Override
    public Map<String, String> getProperties() {
        return Collections.unmodifiableMap(m_properties);
    }

    public void setProperty(final String key, final String value) {
        m_properties.put(key, value);
    }

    @Override
    public String toString() {
        return "MinionContainerConfigurationImpl [pid=" + m_pid + ", properties=" + m_properties + "]";
    }

    public static MinionContainerConfigurationImpl fromConfiguration(final MinionContainerConfiguration minionContainerConfiguration) {
        if (minionContainerConfiguration instanceof MinionContainerConfigurationImpl) {
            return (MinionContainerConfigurationImpl)minionContainerConfiguration;
        }

        final MinionContainerConfigurationImpl impl = new MinionContainerConfigurationImpl(minionContainerConfiguration.getPid());
        for (final Map.Entry<String,String> prop : minionContainerConfiguration.getProperties().entrySet()) {
            impl.setProperty(prop.getKey(), prop.getValue());
        }
        return impl;
    }

    public static List<MinionContainerConfigurationImpl> fromConfigurations(final List<MinionContainerConfiguration> configs) {
        final List<MinionContainerConfigurationImpl> impls = new ArrayList<>();
        for (final MinionContainerConfiguration config : configs) {
            impls.add(MinionContainerConfigurationImpl.fromConfiguration(config));
        }
        return impls;
    }
}
