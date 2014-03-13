package org.opennms.netmgt.api.sample;

import java.util.Set;

public interface MetricRepository {
	Metric getMetric(String metricName);
	Set<Metric> getMetrics(String groupName);
}
