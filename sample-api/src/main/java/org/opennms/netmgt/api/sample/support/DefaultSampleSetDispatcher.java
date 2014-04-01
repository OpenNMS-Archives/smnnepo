package org.opennms.netmgt.api.sample.support;

import org.apache.camel.InOnly;
import org.opennms.netmgt.api.sample.Agent;
import org.opennms.netmgt.api.sample.SampleSet;
import org.opennms.netmgt.api.sample.SampleSetDispatcher;

@InOnly
public class DefaultSampleSetDispatcher extends DefaultDispatcher<SampleSetDispatcher> implements SampleSetDispatcher {

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
