package org.opennms.netmgt.sampler.jmx.internal;

import org.opennms.netmgt.api.sample.CollectionConfiguration;

public class JmxCollectionConfiguration implements CollectionConfiguration<JmxAgent, JmxCollectionRequest> {

    @Override
    public JmxCollectionRequest createRequestForAgent(JmxAgent agent) {
        JmxCollectionRequest request = new JmxCollectionRequest(agent);
        return request;
    }

}
