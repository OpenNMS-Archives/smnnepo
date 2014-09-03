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

import org.opennms.minion.api.MinionContainer;
import org.opennms.minion.api.MinionException;
import org.opennms.minion.api.MinionInitializationMessage;

@XmlRootElement(name="minion-initialization")
@XmlAccessorType(XmlAccessType.NONE)
public class MinionInitializationMessageImpl extends AbstractMinionMessage implements MinionInitializationMessage {
    @XmlAttribute(name="minion-id")
    private String m_minionId;

    @XmlElementWrapper(name="containers")
    @XmlElement(name="container")
    private List<MinionContainerImpl> m_containers = new ArrayList<>();

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

    public List<MinionContainer> getContainers() {
        return Collections.unmodifiableList(new ArrayList<MinionContainer>(m_containers));
    }

    public void addContainer(final MinionContainer minionContainer) {
        m_containers.add(MinionContainerImpl.fromContainer(minionContainer));
    }

    @Override
    public String toString() {
        return "MinionInitializationMessageImpl [minion-id=" + m_minionId + ", containers=" + getContainers() + ", properties=" + getProperties() + "]";
    }
}
