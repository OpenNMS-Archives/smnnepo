package org.opennms.netmgt.api.sample;

import java.util.Set;

public interface MetricRepository {
	public Metric getMetric(String metricName);
	public Set<Metric> getMetrics(String groupName);
}
