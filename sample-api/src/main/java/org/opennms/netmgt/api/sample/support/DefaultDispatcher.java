package org.opennms.netmgt.api.sample.support;

import org.apache.camel.InOnly;
import org.apache.camel.Produce;

@InOnly
public class DefaultDispatcher<T> {

	@Produce(property="endpointUri")
	T m_proxy;

	private final String m_endpointUri;

	public DefaultDispatcher(final String endpointUri) {
	    m_endpointUri = endpointUri;
	}

	public String getEndpointUri() {
		return m_endpointUri;
	}
}
