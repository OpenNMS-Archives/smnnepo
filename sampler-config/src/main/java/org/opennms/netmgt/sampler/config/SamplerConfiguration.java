package org.opennms.netmgt.sampler.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.opennms.netmgt.api.sample.Agent;

public class SamplerConfiguration {
    private List<Agent> m_agents = new ArrayList<Agent>();

    public List<Agent> getAgents(String protocol) {
        return Collections.unmodifiableList(m_agents);
    }

    public void addAgent(final Agent snmpAgent) {
        m_agents.add(snmpAgent);
    }



}
