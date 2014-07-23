package org.opennms.netmgt.sampler.jmx;

import org.opennms.netmgt.api.sample.SampleSet;

public interface JmxCollector {

    SampleSet collect(JmxCollectionRequest request) throws Exception;
}
