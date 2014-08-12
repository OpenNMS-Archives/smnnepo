package org.opennms.netmgt.sampler.jmx.internal;

import org.opennms.netmgt.api.sample.CollectionConfiguration;
import org.opennms.netmgt.api.sample.Metric;
import org.opennms.netmgt.api.sample.MetricRepository;

import java.util.Collections;
import java.util.Set;

// TODO mvr implement me :)
// this may go away... figure that out
public class JmxMetricRepository implements MetricRepository, CollectionConfiguration<JmxAgent, JmxCollectionRequest> {

    // TODO mvr implement me :)
    @Override
    public JmxCollectionRequest createRequestForAgent(JmxAgent agent) {
        JmxCollectionRequest request = new JmxCollectionRequest(agent);
        return request;
    }

    // TODO mvr implement me :)
    // This seems not to be used yet
    @Override
    public Metric getMetric(String metricName) {
        return new Metric() {

        };
    }

    // TODO mvr implement me :)
    // This seems not to be used yet
    @Override
    public Set<Metric> getMetrics(String groupName) {
        return Collections.emptySet();
    }
}
