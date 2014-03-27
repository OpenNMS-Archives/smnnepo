package org.opennms.netmgt.api.sample.support;

import org.apache.camel.InOnly;
import org.apache.camel.Produce;
import org.opennms.netmgt.api.sample.Agent;
import org.opennms.netmgt.api.sample.AgentDispatcher;

@InOnly
public class DefaultAgentDispatcher implements AgentDispatcher {

	@Produce(property="endpointUri")
	AgentDispatcher m_proxy;

	private final String m_endpointUri;

	public DefaultAgentDispatcher(final String endpointUri) {
	    m_endpointUri = endpointUri;
	}

	public String getEndpointUri() {
		return m_endpointUri;
	}

	/**
	 * Send the incoming {@link Agent} message into the Camel route
	 * specified by the {@link #m_endpointUri} property.
	 */
	@Override
	public void dispatch(final Agent agent) {
		m_proxy.dispatch(agent);
	}
}
