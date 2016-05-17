package org.opennms.netmgt.api.sample.support;

import org.apache.camel.Produce;
import org.opennms.core.camel.DefaultDispatcher;
import org.opennms.netmgt.api.sample.Agent;
import org.opennms.netmgt.api.sample.SampleSet;
import org.opennms.netmgt.api.sample.SampleSetDispatcher;

public class DefaultSampleSetDispatcher extends DefaultDispatcher implements SampleSetDispatcher {

	@Produce(property="endpointUri")
	SampleSetDispatcher m_proxy;

	public DefaultSampleSetDispatcher(final String endpointUri) {
		super(endpointUri);
	}

	/**
	 * Send the incoming {@link Agent} message into the Camel route
	 * specified by the {@link #m_endpointUri} property.
	 */
	@Override
	public void save(final SampleSet sampleSet) {
		m_proxy.save(sampleSet);
	}
}
