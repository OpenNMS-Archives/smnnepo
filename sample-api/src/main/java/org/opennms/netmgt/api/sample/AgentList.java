package org.opennms.netmgt.api.sample;

import java.util.Collection;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.opennms.core.config.api.JaxbListWrapper;

@XmlRootElement(name="agents")
@XmlAccessorType(XmlAccessType.NONE)
public class AgentList extends JaxbListWrapper<Agent> {
    private static final long serialVersionUID = 3L;

    public AgentList() { super(); }
    public AgentList(final Collection<? extends Agent> agents) {
        super(agents);
    }

    @XmlElement(name="agent")
    public List<Agent> getObjects() {
        return super.getObjects();
    }
}