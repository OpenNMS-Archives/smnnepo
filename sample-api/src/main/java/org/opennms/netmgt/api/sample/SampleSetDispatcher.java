package org.opennms.netmgt.api.sample;

import org.apache.camel.InOnly;

@InOnly
public interface SampleSetDispatcher {

	void save(SampleSet samples);
}
