package org.opennms.netmgt.api.sample;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="agents")
@XmlAccessorType(XmlAccessType.NONE)
public class AgentList extends ArrayList<Agent> {
    private static final long serialVersionUID = 2L;

    @XmlElement(name="agent")
    public List<Agent> getAgents() {
        return this;
    }

    public void setAgents(final List<Agent> agents) {
        if (agents == this) {
            return;
        }
        clear();
        addAll(agents);
    }
}