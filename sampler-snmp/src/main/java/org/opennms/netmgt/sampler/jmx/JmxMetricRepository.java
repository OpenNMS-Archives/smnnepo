package org.opennms.netmgt.sampler.jmx;

import org.opennms.netmgt.api.sample.CollectionConfiguration;
import org.opennms.netmgt.api.sample.Metric;
import org.opennms.netmgt.api.sample.MetricRepository;

import java.util.Set;

public class JmxMetricRepository implements MetricRepository, CollectionConfiguration<JmxAgent, JmxCollectionRequest> {
    @Override
    public JmxCollectionRequest createRequestForAgent(JmxAgent agent) {
        return null;
    }

    @Override
    public Metric getMetric(String metricName) {
        return null;
    }

    @Override
    public Set<Metric> getMetrics(String groupName) {
        return null;
    }
}
