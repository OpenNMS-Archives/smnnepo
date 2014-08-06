package org.opennms.minion.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import org.opennms.minion.api.Container;
import org.opennms.minion.api.MinionException;
import org.opennms.minion.api.MinionInitializationMessage;

@XmlRootElement(name="minion-initialization")
@XmlAccessorType(XmlAccessType.NONE)
public class MinionInitializationMessageImpl extends AbstractMinionMessage implements MinionInitializationMessage {
    @XmlAttribute(name="minion-id")
    private String m_minionId;

    @XmlElementWrapper(name="feature-repositories")
    @XmlElement(name="repository")
    private List<String> m_featureRepositories = new ArrayList<>();

    @XmlElementWrapper(name="containers")
    @XmlElement(name="container")
    private List<ContainerImpl> m_containers = new ArrayList<>();

    protected MinionInitializationMessageImpl() {
        super();
    }

    public MinionInitializationMessageImpl(final String minionId, final Integer version) throws MinionException {
        super(version);
        if (minionId == null) {
            throw new MinionException("Minion ID must be defined!");
        }
        m_minionId = minionId;
    }

    @Override
    public String getMinionId() {
        return m_minionId;
    }

    @Override
    public List<String> getFeatureRepositories() {
        return m_featureRepositories;
    }

    public void setFeatureRepositories(final List<String> featureRepositories) {
        if (m_featureRepositories == featureRepositories) {
            return;
        }
        m_featureRepositories.clear();
        m_featureRepositories.addAll(featureRepositories);
    }

    public void addFeatureRepository(final String repository) {
        m_featureRepositories.add(repository);
    }

    public List<Container> getContainers() {
        return Collections.unmodifiableList(new ArrayList<Container>(m_containers));
    }

    public void addContainer(final Container container) {
        m_containers.add(ContainerImpl.fromContainer(container));
    }

    @Override
    public String toString() {
        return "MinionInitializationMessageImpl [minion-id=" + m_minionId + ", feature-repositories=" + m_featureRepositories + ", properties=" + getProperties() + "]";
    }
}
