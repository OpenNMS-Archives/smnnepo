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

import org.opennms.minion.api.ContainerConfiguration;
import org.opennms.minion.api.MapAdapter;

@XmlRootElement(name="configuration")
@XmlAccessorType(XmlAccessType.NONE)
public class ContainerConfigurationImpl implements ContainerConfiguration {
    @XmlAttribute(name="pid")
    private String m_pid;

    @XmlElement(name="properties")
    @XmlJavaTypeAdapter(MapAdapter.class)
    private Map<String,String> m_properties = new LinkedHashMap<>();

    protected ContainerConfigurationImpl() {
    }

    public ContainerConfigurationImpl(final String pid) {
        m_pid = pid;
    }

    public static ContainerConfigurationImpl fromConfiguration(final ContainerConfiguration containerConfiguration) {
        if (containerConfiguration instanceof ContainerConfigurationImpl) {
            return (ContainerConfigurationImpl)containerConfiguration;
        }

        final ContainerConfigurationImpl impl = new ContainerConfigurationImpl(containerConfiguration.getPid());
        for (final Map.Entry<String,String> prop : containerConfiguration.getProperties().entrySet()) {
            impl.setProperty(prop.getKey(), prop.getValue());
        }
        return impl;
    }

    public static List<ContainerConfigurationImpl> fromConfigurations(final List<ContainerConfiguration> configs) {
        final List<ContainerConfigurationImpl> impls = new ArrayList<>();
        for (final ContainerConfiguration config : configs) {
            impls.add(ContainerConfigurationImpl.fromConfiguration(config));
        }
        return impls;
    }

    @Override
    public String getPid() {
        return m_pid;
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
        return "ContainerConfigurationImpl [pid=" + m_pid + ", properties=" + m_properties + "]";
    }
}
