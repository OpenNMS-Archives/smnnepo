package org.opennms.netmgt.sampler.config.snmp;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.opennms.netmgt.api.sample.AgentRepository;

public class SnmpAgentRepository implements AgentRepository<SnmpAgent> {
	private Map<String, SnmpAgent> m_agents = new HashMap<String, SnmpAgent>();
	
	public void addAgent(SnmpAgent agent) {
		m_agents.put(agent.getId(), agent);
	}

	@Override
	public Collection<SnmpAgent> getAgentsByProtocol(String protocol) {
		return m_agents.values();
	}

	@Override
	public SnmpAgent getAgentById(String agentId) {
		return m_agents.get(agentId);
	}
}
