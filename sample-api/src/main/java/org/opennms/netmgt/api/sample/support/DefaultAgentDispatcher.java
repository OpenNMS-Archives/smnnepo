package org.opennms.netmgt.api.sample.support;

import org.apache.camel.InOnly;
import org.opennms.netmgt.api.sample.Agent;
import org.opennms.netmgt.api.sample.AgentDispatcher;

@InOnly
public class DefaultAgentDispatcher extends DefaultDispatcher<AgentDispatcher> implements AgentDispatcher {

	public DefaultAgentDispatcher(final String endpointUri) {
		super(endpointUri);
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
