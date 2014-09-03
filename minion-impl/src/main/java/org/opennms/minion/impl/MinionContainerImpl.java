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

import org.opennms.minion.api.MinionContainer;
import org.opennms.minion.api.MinionContainerConfiguration;

@XmlRootElement(name="container")
@XmlAccessorType(XmlAccessType.NONE)
public class MinionContainerImpl implements MinionContainer {
    @XmlAttribute(name="name")
    private String m_name;
    
    @XmlAttribute(name="pid")
    private String m_pid;

    @XmlElementWrapper(name="features")
    @XmlElement(name="feature")
    private List<String> m_features = new ArrayList<>();

    @XmlElementWrapper(name="feature-repositories")
    @XmlElement(name="feature-repository")
    private List<String> m_featureRepositories = new ArrayList<>();

    @XmlElement(name="script")
    private String m_script;
    
    @XmlElementWrapper(name="script-arguments")
    @XmlElement(name="argument")
    private List<String> m_scriptArguments;

    @XmlElementWrapper(name="configurations")
    @XmlElement(name="configuration")
    private List<MinionContainerConfigurationImpl> m_containerConfigurations = new ArrayList<>();

    protected MinionContainerImpl() {
    }

    public MinionContainerImpl(final String name, final String pid) {
        m_name = name;
        m_pid = pid;
    }

    public String getName() {
        return m_name;
    }
    
    public String getPid() {
        return m_pid;
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

    @Override
    public List<String> getFeatureRepositories() {
        return m_featureRepositories;
    }

    public void setFeatureRepositories(final List<String> repositories) {
        if (m_featureRepositories == repositories) {
            return;
        }
        m_featureRepositories.clear();
        m_featureRepositories.addAll(repositories);
    }

    public void addFeatureRepository(final String featureRepository) {
        m_featureRepositories.add(featureRepository);
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

    public List<MinionContainerConfiguration> getConfigurations() {
        if (m_containerConfigurations  == null) {
            return Collections.emptyList();
        } else {
            return Collections.unmodifiableList(new ArrayList<MinionContainerConfiguration>(m_containerConfigurations));
        }
    }

    public void setConfigurations(final List<MinionContainerConfiguration> configs) {
        m_containerConfigurations = new ArrayList<>(MinionContainerConfigurationImpl.fromConfigurations(configs));
    }

    public void addConfiguration(final MinionContainerConfiguration minionContainerConfiguration) {
        m_containerConfigurations.add(MinionContainerConfigurationImpl.fromConfiguration(minionContainerConfiguration));
    }

    public void setConfigurationProperty(final String pid, final String key, final String value) {
        MinionContainerConfigurationImpl config = null;
        for (final MinionContainerConfiguration c : m_containerConfigurations) {
            if (pid.equals(c.getPid())) {
                config = MinionContainerConfigurationImpl.fromConfiguration(c);
                break;
            }
        }
        
        if (config == null) {
            config = new MinionContainerConfigurationImpl(pid);
            m_containerConfigurations.add(config);
        }
        config.setProperty(key, value);
    }

    @Override
    public String toString() {
        return "MinionContainerImpl [name=" + m_name + ", pid=" + m_pid + ", features=" + m_features + ", featureRepositories=" + m_featureRepositories + ", script=" + m_script
                + ", scriptArguments=" + m_scriptArguments + ", containerConfigurations=" + m_containerConfigurations + "]";
    }

    public static MinionContainerImpl fromContainer(final MinionContainer minionContainer) {
        if (minionContainer instanceof MinionContainerImpl) {
            return (MinionContainerImpl)minionContainer;
        }
        
        final MinionContainerImpl newContainer = new MinionContainerImpl(minionContainer.getName(), minionContainer.getPid());
        newContainer.setScript(minionContainer.getScript());
        newContainer.setScriptArguments(minionContainer.getScriptArguments());
        newContainer.setConfigurations(minionContainer.getConfigurations());
        newContainer.setFeatures(minionContainer.getFeatures());
        newContainer.setFeatureRepositories(minionContainer.getFeatureRepositories());

        return newContainer;
    }
}
