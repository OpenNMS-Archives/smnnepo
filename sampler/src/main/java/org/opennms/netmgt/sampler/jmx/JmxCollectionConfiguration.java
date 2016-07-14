package org.opennms.netmgt.sampler.jmx;

import org.opennms.netmgt.api.sample.CollectionConfiguration;
import org.opennms.netmgt.sampler.jmx.internal.JmxAgent;
import org.opennms.netmgt.sampler.jmx.internal.JmxCollectionRequest;

public class JmxCollectionConfiguration implements CollectionConfiguration<JmxAgent, JmxCollectionRequest> {

    @Override
    public JmxCollectionRequest createRequestForAgent(JmxAgent agent) {
        JmxCollectionRequest request = new JmxCollectionRequest(agent);
        return request;
    }

}
