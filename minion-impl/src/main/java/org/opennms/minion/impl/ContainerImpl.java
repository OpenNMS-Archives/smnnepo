package org.opennms.minion.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import org.opennms.minion.api.Container;
import org.opennms.minion.api.ContainerConfiguration;

@XmlRootElement(name="container")
@XmlAccessorType(XmlAccessType.NONE)
public class ContainerImpl implements Container {
    @XmlAttribute(name="name")
    private String m_name;
    
    @XmlElementWrapper(name="features")
    @XmlElement(name="feature")
    private List<String> m_features = new ArrayList<>();

    @XmlElement(name="script")
    private String m_script;
    
    @XmlElementWrapper(name="script-arguments")
    @XmlElement(name="argument")
    private List<String> m_scriptArguments;

    @XmlElementWrapper(name="configurations")
    @XmlElement(name="configuration")
    private List<ContainerConfigurationImpl> m_containerConfigurations = new ArrayList<>();

    protected ContainerImpl() {
    }

    public ContainerImpl(final String name) {
        m_name = name;
    }

    public String getName() {
        return m_name;
    }

    @Override
    public List<String> getFeatures() {
        return m_features;
    }

    public void setFeatures(final List<String> features) {
        if (m_features == features) {
            return;
        }
        m_features.clear();
        m_features.addAll(features);
    }

    public void addFeature(final String feature) {
        m_features.add(feature);
    }

    public String getScript() {
        return m_script;
    }
    
    public void setScript(final String script) {
        m_script = script;
    }

    public List<String>getScriptArguments() {
        return m_scriptArguments;
    }

    public void setScriptArguments(final List<String> args) {
        m_scriptArguments = new ArrayList<>(args);
    }

    public void setScriptArguments(final String... args) {
        m_scriptArguments = Arrays.asList(args);
    }

    public List<ContainerConfiguration> getConfigurations() {
        if (m_containerConfigurations  == null) {
            return Collections.emptyList();
        } else {
            return Collections.unmodifiableList(new ArrayList<ContainerConfiguration>(m_containerConfigurations));
        }
    }

    public void setConfigurations(final List<ContainerConfiguration> configs) {
        m_containerConfigurations = new ArrayList<>(ContainerConfigurationImpl.fromConfigurations(configs));
    }

    public void addConfiguration(final ContainerConfiguration containerConfiguration) {
        m_containerConfigurations.add(ContainerConfigurationImpl.fromConfiguration(containerConfiguration));
    }

    public void setConfigurationProperty(final String pid, final String key, final String value) {
        ContainerConfigurationImpl config = null;
        for (final ContainerConfiguration c : m_containerConfigurations) {
            if (pid.equals(c.getPid())) {
                config = ContainerConfigurationImpl.fromConfiguration(c);
                break;
            }
        }
        
        if (config == null) {
            config = new ContainerConfigurationImpl(pid);
        }
        config.setProperty(key, value);
    }

    public static ContainerImpl fromContainer(final Container container) {
        if (container instanceof ContainerImpl) {
            return (ContainerImpl)container;
        }
        
        final ContainerImpl newContainer = new ContainerImpl(container.getName());
        newContainer.setScript(container.getScript());
        newContainer.setScriptArguments(container.getScriptArguments());
        newContainer.setConfigurations(container.getConfigurations());

        return newContainer;
    }
}
