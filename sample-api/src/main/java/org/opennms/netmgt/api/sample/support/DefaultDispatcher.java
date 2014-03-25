package org.opennms.netmgt.api.sample.support;

import org.apache.camel.InOnly;
import org.apache.camel.Produce;
import org.opennms.netmgt.api.sample.Agent;
import org.opennms.netmgt.api.sample.Dispatcher;

@InOnly
public class DefaultDispatcher implements Dispatcher {

	@Produce(property="endpointUri")
	Dispatcher m_proxy;

	private final String m_endpointUri;

	public DefaultDispatcher(final String endpointUri) {
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
