package org.opennms.netmgt.api.sample;

import java.util.Collection;

public interface AgentRepository<T extends Agent> {
	
	void addAgent(T agent);
	
	Collection<T> getAgentsByProtocol(String protocol);

	T getAgentById(String agentId);
}
