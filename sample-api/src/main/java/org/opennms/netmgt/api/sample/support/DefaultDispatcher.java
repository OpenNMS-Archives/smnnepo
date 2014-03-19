package org.opennms.netmgt.api.sample.support;

import org.apache.camel.InOnly;
import org.apache.camel.Produce;
import org.opennms.netmgt.api.sample.Agent;
import org.opennms.netmgt.api.sample.Dispatcher;

@InOnly
public class DefaultDispatcher implements Dispatcher {

	@Produce(property="endpointUri")
	Dispatcher proxy;

	private String m_endpointUri;

	public String getEndpointUri() {
		return m_endpointUri;
	}

	public void setEndpointUri(String endpointUri) {
		this.m_endpointUri = endpointUri;
	}

	/**
	 * Send the incoming {@link Agent} message into the Camel route
	 * specified by the {@link #m_endpointUri} property.
	 */
	@Override
	public void dispatch(Agent agent) {
		proxy.dispatch(agent);
	}
}
